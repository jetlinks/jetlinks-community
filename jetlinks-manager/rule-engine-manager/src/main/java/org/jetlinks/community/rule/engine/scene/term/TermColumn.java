package org.jetlinks.community.rule.engine.scene.term;

import com.fasterxml.jackson.annotation.JsonIgnore;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;
import org.apache.commons.collections4.CollectionUtils;
import org.hswebframework.ezorm.core.param.Term;
import org.hswebframework.web.bean.FastBeanCopier;
import org.jetlinks.core.metadata.DataType;
import org.jetlinks.core.metadata.PropertyMetadata;
import org.jetlinks.core.metadata.types.EnumType;
import org.jetlinks.community.PropertyMetadataConstants;
import org.jetlinks.community.PropertyMetric;
import org.jetlinks.community.rule.engine.scene.DeviceOperation;
import org.springframework.util.StringUtils;
import org.jetlinks.community.reactorql.term.TermType;
import org.jetlinks.community.reactorql.term.TermTypes;

import java.util.*;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;

@Getter
@Setter
public class TermColumn {

    @Schema(description = "条件列")
    private String column;

    @Schema(description = "名称")
    private String name;

    @Schema(description = "全名")
    private String fullName;

    @Schema(description = "说明")
    private String description;

    @Schema(description = "数据类型")
    private String dataType;

    @Schema(description = "是否为物模型列")
    private boolean metadata;

    /**
     * @see Term#getTermType()
     */
    @Schema(description = "支持的条件类型")
    private List<TermType> termTypes;

    @Schema(description = "支持的指标")
    private List<PropertyMetric> metrics;

    @Schema(description = "可选内容")
    private List<PropertyMetric> options;

    @Schema(description = "其它配置")
    private Map<String, Object> others;

    @Schema(description = "子列,在类型为object时有值")
    private List<TermColumn> children;

    public TermColumn withMetadataTrue() {
        metadata = true;
        return this;
    }

    public TermColumn copyColumn(Predicate<String> childrenPredicate) {
        TermColumn copy = FastBeanCopier.copy(this, new TermColumn());

        if (CollectionUtils.isNotEmpty(children)) {
            copy.setChildren(
                children.stream()
                        .filter(child -> childrenPredicate.test(child.getColumn()))
                        .map(child -> child.copyColumn(childrenPredicate))
                        .collect(Collectors.toList())
            );
        }

        return copy;
    }

    public boolean hasColumn(Collection<String> columns) {
        for (String column : columns) {
            if (hasColumn(column)) {
                return true;
            }
        }
        return false;
    }

    public boolean hasColumn(String column) {
        if (this.column.equals(column)) {
            return true;
        }
        if (children == null) {
            return false;
        }
        for (TermColumn child : children) {
            if (child.hasColumn(column)) {
                return true;
            }
        }
        return false;
    }

    public String getVariable(CharSequence delimiter) {
        if (column == null) {
            return null;
        }
        String[] arr = column.split("[.]");
        if (arr.length == 1) {
            return arr[0];
        }
        return String.join(delimiter, Arrays.copyOfRange(arr, 1, arr.length));
    }

    @JsonIgnore
    public String getPropertyOrNull() {
        if (column == null) {
            return null;
        }
        if (!column.startsWith("properties")) {
            return null;
        }
        String[] arr = column.split("[.]");
        return arr[1];
    }

    public PropertyMetric getMetricOrNull(String metric) {
        if (this.metrics == null) {
            return null;
        }
        for (PropertyMetric propertyMetric : this.metrics) {
            if (Objects.equals(propertyMetric.getId(), metric)) {
                return propertyMetric;
            }
        }
        return null;
    }

    public TermColumn with(PropertyMetadata metadata) {
        setColumn(metadata.getId());
        setName(metadata.getName());
        setDataType(metadata.getValueType().getId());
        withMetrics(metadata);
        setTermTypes(TermTypes.lookup(metadata.getValueType()));
        return this;
    }

    public TermColumn withMetrics(PropertyMetadata metadata) {
        return withMetrics(PropertyMetadataConstants.Metrics.getMetrics(metadata));
    }

    public TermColumn withMetrics(List<PropertyMetric> metrics) {
        this.metrics = metrics;
        return this;
    }

    public static TermColumn of(String column, String name, DataType type) {
        return of(column, name, type, null);
    }

    public static TermColumn of(String column, String name, DataType type, String description) {
        TermColumn termColumn = new TermColumn();
        termColumn.setColumn(column);
        termColumn.setName(name);
        termColumn.setDataType(type.getId());
        termColumn.setDescription(description);
        termColumn.setTermTypes(TermTypes.lookup(type));
        if (type instanceof EnumType) {
            List<EnumType.Element> elements = ((EnumType) type).getElements();
            if (CollectionUtils.isNotEmpty(elements)) {
                List<PropertyMetric> options = new ArrayList<>(elements.size());
                for (EnumType.Element element : elements) {
                    options.add(PropertyMetric.of(element.getValue(), element.getText(), null));
                }
                termColumn.setOptions(options);
            }
        }
        return termColumn;
    }

    public static TermColumn of(PropertyMetadata metadata) {
        return new TermColumn().with(metadata);
    }

    public void refactorDescription(Function<String, TermColumn> columnGetter) {
        if (!StringUtils.hasText(description)) {
            doRefactorDescription(columnGetter);
        }
        if (children != null) {
            for (TermColumn child : children) {
                child.refactorDescription(columnGetter);
            }
        }
    }

    public void doRefactorDescription(Function<String, TermColumn> columnGetter) {
        if (CollectionUtils.isNotEmpty(children)) {
            return;
        }
        //属性
        if (column.startsWith("properties")) {
            String[] arr = column.split("[.]");
            if (arr.length > 3) {
                TermColumn column = columnGetter.apply(arr[1]);
                //类型,report,recent,latest
                String type = arr[arr.length - 1];
                setDescription(
                    DeviceOperation.PropertyValueType
                        .valueOf(type)
                        .getNestDescription(column.name)
                );
            } else {
                String type = arr[arr.length - 1];
                setDescription(
                    DeviceOperation.PropertyValueType
                        .valueOf(type)
                        .getDescription()
                );
            }
        }
    }

    public void refactorFullName(String parentName) {
        if (StringUtils.hasText(parentName)) {
            this.fullName = parentName + "/" + name;
        } else {
            this.fullName = name;
        }
        if (CollectionUtils.isNotEmpty(children)) {
            for (TermColumn child : children) {
                child.refactorFullName(this.fullName);
            }
        }
    }

    public TermColumn withOthers(Map<String, Object> options) {
        if (options != null) {
            safeOptions().putAll(options);
        }
        return this;
    }

    public synchronized Map<String, Object> safeOptions() {
        return others == null ? others = new HashMap<>() : others;
    }

}

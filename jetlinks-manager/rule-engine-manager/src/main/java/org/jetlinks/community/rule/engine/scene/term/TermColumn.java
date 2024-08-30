package org.jetlinks.community.rule.engine.scene.term;

import com.fasterxml.jackson.annotation.JsonIgnore;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;
import org.apache.commons.collections4.CollectionUtils;
import org.hswebframework.ezorm.core.param.Term;
import org.hswebframework.web.bean.FastBeanCopier;
import org.hswebframework.web.i18n.LocaleUtils;
import org.jetlinks.core.metadata.DataType;
import org.jetlinks.core.metadata.PropertyMetadata;
import org.jetlinks.core.metadata.types.BooleanType;
import org.jetlinks.core.metadata.types.EnumType;
import org.jetlinks.community.PropertyMetadataConstants;
import org.jetlinks.community.PropertyMetric;
import org.jetlinks.community.rule.engine.scene.DeviceOperation;
import org.jetlinks.community.terms.I18nSpec;
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

    @Schema(description = "名称编码")
    private String code;

    @Schema(description = "名称")
    private String name;

    @Schema(description = "全名")
    private String fullName;

    @Schema(description = "全名显码")
    private I18nSpec fullNameCode;

    @Schema(description = "描述编码")
    private String codeDesc;

    @Schema(description = "说明")
    private String description;

    @Schema(description = "数据类型")
    private String dataType;

    @Schema(description = "是否为物模型列")
    private boolean metadata;

    @Schema(description = "物模型各层级变量名称集合。如：['事件名称','事件属性']")
    private List<String> metadataHierarchyNames;

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

    public static TermColumn of(String column, String code, String defaultName, DataType type) {
        TermColumn termColumn = of(column, defaultName, type, null);
        termColumn.setCode(code);
        return termColumn;
    }


    public static TermColumn of(String column,
                                String code,
                                String defaultName,
                                DataType type,
                                String defaultDescription) {
        TermColumn termColumn = of(column, defaultName, type, resolveI18n(getDescriptionByCode(code), defaultDescription));
        termColumn.setCode(code);
        termColumn.setCodeDesc(getDescriptionByCode(code));
        return termColumn;
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
                termColumn.withOther("elements", elements);
            }
        }
        if (type instanceof BooleanType) {
            termColumn.withOther("bool", type);
        }
        return termColumn;
    }

    public static TermColumn of(PropertyMetadata metadata) {
        return new TermColumn().with(metadata);
    }

    public static List<TermColumn> refactorTermsInfo(String perText, List<TermColumn> terms) {
        Map<String, TermColumn> allColumn = terms
                .stream()
                .collect(Collectors.toMap(TermColumn::getColumn, Function.identity(), (a, b) -> a));
        for (TermColumn term : terms) {

            term.refactorDescription(perText, allColumn::get);
            term.refactorFullName(null);
        }
        return terms;
    }

    public void refactorDescription(String perText, Function<String, TermColumn> columnGetter) {
        if (!StringUtils.hasText(description)) {
            doRefactorDescription(perText, columnGetter);
        }
        if (children != null) {
            for (TermColumn child : children) {
                child.refactorDescription(perText, columnGetter);
            }
        }
    }

    public void doRefactorDescription(String perText, Function<String, TermColumn> columnGetter) {
        if (CollectionUtils.isNotEmpty(children)) {
            return;
        }
        //属性或采集器数据
        if (column.startsWith(perText)) {
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
            //code不为空，表示当前termColumn需要国际化
            if (StringUtils.hasText(code)) {
                this.fullNameCode = I18nSpec
                        .of("message.scene_term_column_full_name", null, parentName)
                        .withArgs(code, name);
            } else {
                this.fullNameCode = I18nSpec
                    .of("message.scene_term_column_full_name", null, parentName, name);
            }
            this.fullName = fullNameCode.resolveI18nMessage();
        } else {
            this.fullName = name;
            if (CollectionUtils.isEmpty(children)) {
                if (StringUtils.hasText(code)) {
                    this.fullNameCode = I18nSpec.of(code, name);
                    this.fullName = fullNameCode.resolveI18nMessage();
                } else {
                    this.fullName = name;
                }
            }
        }
        if (CollectionUtils.isNotEmpty(children)) {
            for (TermColumn child : children) {
                child.refactorFullName(this.fullName);
            }
        }
    }

    public TermColumn withOther(String key, Object value) {
        safeOptions().put(key, value);
        return this;
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


    private static String getDescriptionByCode(String code) {
        return code + "_desc";
    }

    private static String resolveI18n(String key, String name) {
        if (StringUtils.hasText(name)) {
            return LocaleUtils.resolveMessage(key, name);
        }
        return LocaleUtils.resolveMessage(key);
    }
}

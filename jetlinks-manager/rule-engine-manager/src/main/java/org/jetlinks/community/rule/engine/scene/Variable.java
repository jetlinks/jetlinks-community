package org.jetlinks.community.rule.engine.scene;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;
import org.apache.commons.collections4.CollectionUtils;
import org.hswebframework.ezorm.core.param.Term;
import org.jetlinks.core.metadata.DataType;
import org.jetlinks.core.metadata.types.StringType;
import org.jetlinks.community.reactorql.term.TermType;
import org.jetlinks.community.reactorql.term.TermTypes;
import org.jetlinks.community.rule.engine.scene.term.TermColumn;
import org.jetlinks.community.terms.I18nSpec;
import org.springframework.util.StringUtils;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Getter
@Setter
public class Variable {
    public static final String OPTION_PRODUCT_ID = "productId";
    //用于标记前端从后续请求接口（如告警）内取参名称
    public static final String OPTION_PARAMETER = "parameter";
    @Schema(description = "变量ID")
    private String id;

    @Schema(description = "变量名")
    private String name;

    @Schema(description = "变量编码")
    private String code;

    @Schema(description = "变量全名")
    private String fullName;

    @Schema(description = "全名显码")
    private I18nSpec fullNameCode;

    @Schema(description = "列")
    private String column;

    @Schema(description = "说明")
    private String description;

    @Schema(description = "类型")
    private String type;

    /**
     * @see Term#getTermType()
     */
    @Schema(description = "支持的条件类型")
    private List<TermType> termTypes;

    @Schema(description = "子级变量")
    private List<Variable> children;

    @Schema(description = "是否为物模型变量")
    private boolean metadata;

    @Schema(description = "其他配置")
    private Map<String, Object> options;

    public String getFullName() {
        return fullName == null ? name : fullName;
    }

    public Variable withDescription(String description) {
        this.description = description;
        return this;
    }

    public Variable withMetadata(boolean metadata) {
        this.metadata = metadata;
        return this;
    }

    public synchronized Map<String, Object> safeOptions() {
        return options == null ? options = new HashMap<>() : options;
    }

    public Variable withOption(String key, Object value) {
        safeOptions().put(key, value);
        return this;
    }

    public Variable withOptions(Map<String, Object> options) {
        safeOptions().putAll(options);
        return this;
    }

    public Variable withType(String type) {
        this.type = type;
        return this;
    }

    public Variable withType(DataType type) {
        withType(type.getId())
                .withTermType(TermTypes.lookup(type));
        return this;
    }

    public Variable withTermType(List<TermType> termTypes) {
        this.termTypes = termTypes;
        return this;
    }

    public Variable withColumn(String column) {
        this.column = column;
        return this;
    }

    public Variable withFullNameCode(I18nSpec fullNameCode) {
        this.fullNameCode = fullNameCode;
        return this;
    }

    public Variable withCode(String code) {
        this.code = code;
        return this;
    }

    public String getColumn() {
        if (StringUtils.hasText(column)) {
            return column;
        }
        return id;
    }

    public Variable with(TermColumn column) {
        this.name = column.getName();
        this.code = column.getCode();
        this.column = column.getColumn();
        this.metadata = column.isMetadata();
        this.description = column.getDescription();
        this.fullName = column.getFullName();
        this.fullNameCode = column.getFullNameCode() == null ? null : column.getFullNameCode().copy();
        this.type = column.getDataType();
        this.termTypes = column.getTermTypes();
        withOptions(column.safeOptions());
        return this;
    }


    public void refactorPrefix() {
        refactorPrefix(this);
    }

    public void refactorPrefix(Variable main) {
        id = SceneUtils.transferSceneUpperKey(id);
        if (CollectionUtils.isNotEmpty(children)) {
            for (Variable child : children) {
                if (!child.getId().startsWith(main.id + ".")) {
                    child.setId(main.id + "." + child.getId());
                }
                child.setId(SceneUtils.transferSceneUpperKey(child.getId()));

                if (StringUtils.hasText(child.getFullName()) && StringUtils.hasText(main.getFullName())) {
                    child.setFullName(main.getFullName() + "/" + child.getFullName());
                }

                child.refactorPrefix(main);
            }
        }
    }

    private Variable(String id, String name) {
        this.id = id;
        this.name = name;
        this.description = name;
        this.type = StringType.ID;
    }

    public Variable() {
    }

    public static Variable of(String id, String name) {
        return new Variable(id, name);
    }

}

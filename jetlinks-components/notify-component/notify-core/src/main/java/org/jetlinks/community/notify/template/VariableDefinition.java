package org.jetlinks.community.notify.template;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hswebframework.utils.time.DateFormatter;
import org.hswebframework.web.exception.ValidationException;
import org.jetlinks.core.config.ConfigKey;
import org.jetlinks.core.metadata.Converter;
import org.jetlinks.core.metadata.DataType;
import org.jetlinks.core.metadata.types.*;
import org.jetlinks.community.ConfigMetadataConstants;
import org.jetlinks.community.relation.utils.VariableSource;
import org.springframework.util.StringUtils;

import javax.validation.constraints.NotBlank;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.function.Supplier;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class VariableDefinition {

    @Schema(description = "变量标识")
    @NotBlank
    private String id;

    @Schema(description = "变量名称")
    private String name;

    @Schema(description = "说明")
    private String description;

    /**
     * @see DataType#getId()
     * @see org.jetlinks.core.metadata.types.DoubleType
     * @see DateTimeType
     * @see org.jetlinks.core.metadata.types.FileType
     */
    @Schema(description = "类型")
    private String type;

    @Schema(description = "是否必选")
    private boolean required;

    /**
     * @see Variable.FileFormat
     */
    @Schema(description = "格式")
    private String format;

    @Schema(description = "其他配置")
    private Map<String, Object> expands;

    @Schema(description = "默认值")
    private String defaultValue;

    public static VariableDefinitionBuilder builder() {
        return new VariableDefinitionBuilder();
    }

    public String convertValue(Object value) {
        if (value == null) {
            value = defaultValue;
        }
        //必填
        if (this.required && value == null) {
            throw new ValidationException(id, "error.template_var_required", this.getId(), this.getName());
        }
        DataType dataType = lookupType();
        String fmt = format;
        Object val = value;
        //为null时返回空字符
        if (val == null) {
            return "";
        }
        //日期格式
        if (dataType instanceof DateTimeType) {
            Date date = ((DateTimeType) dataType).convert(val);
            if ("timestamp".equals(fmt)) {
                return String.valueOf(date.getTime());
            }
            if (fmt == null) {
                fmt = "yyyy-MM-dd HH:mm:ss";
            }
            return DateFormatter.toString(date, fmt);
        }
        //其他格式
        if (dataType instanceof Converter) {
            val = ((Converter<?>) dataType).convert(val);
        }
        if (StringUtils.hasText(fmt) && fmt.contains("%")) {
            if (val instanceof Number) {
                val = ((Number) val).doubleValue();
            }
            return String.format(fmt, val);
        }
        return String.valueOf(val);
    }

    private DataType lookupType() {
        DataType dataType = Optional
            .ofNullable(DataTypes.lookup(type))
            .map(Supplier::get)
            .orElse(StringType.GLOBAL);
        if (dataType instanceof AbstractType) {
            ((AbstractType<?>) dataType).expands(expands);
            if (StringUtils.hasText(format)) {
                ((AbstractType<?>) dataType).expand(ConfigMetadataConstants.format, format);
            }
        }
        return dataType;
    }

    public static class VariableDefinitionBuilder {
        private @NotBlank String id;
        private String name;
        private String description;
        private String type;
        private boolean required;
        private String format;
        private final Map<String, Object> expands = new HashMap<>();
        private String defaultValue;

        VariableDefinitionBuilder() {
        }

        public VariableDefinitionBuilder id(@NotBlank String id) {
            this.id = id;
            return this;
        }

        public VariableDefinitionBuilder name(String name) {
            this.name = name;
            return this;
        }

        public VariableDefinitionBuilder description(String description) {
            this.description = description;
            return this;
        }

        public VariableDefinitionBuilder type(String type) {
            this.type = type;
            return this;
        }

        public VariableDefinitionBuilder type(DataType type) {
            this.type = type.getType();
            return expands(type.getExpands());
        }


        public VariableDefinitionBuilder required(boolean required) {
            this.required = required;
            return this;
        }

        public VariableDefinitionBuilder format(String format) {
            this.format = format;
            return this;
        }

        public VariableDefinitionBuilder expands(Map<String, Object> expands) {
            if (expands == null) {
                return this;
            }
            this.expands.putAll(expands);
            return this;
        }

        public VariableDefinitionBuilder expand(ConfigKey<String> key, Object value) {
            this.expands.put(key.getKey(), value);
            return this;
        }

        public VariableDefinitionBuilder expand(String key, Object value) {
            this.expands.put(key, value);
            return this;
        }

        public VariableDefinitionBuilder defaultValue(String defaultValue) {
            this.defaultValue = defaultValue;
            return this;
        }

        public VariableDefinition build() {
            return new VariableDefinition(id, name, description, type, required, format, expands, defaultValue);
        }

        public String toString() {
            return "VariableDefinition.VariableDefinitionBuilder(id=" + this.id + ", name=" + this.name + ", description=" + this.description + ", type=" + this.type + ", required=" + this.required + ", format=" + this.format + ", expands=" + this.expands + ", defaultValue=" + this.defaultValue + ")";
        }
    }
}
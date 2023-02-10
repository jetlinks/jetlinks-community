package org.jetlinks.community;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;
import org.hswebframework.web.bean.FastBeanCopier;
import org.jetlinks.community.utils.ConverterUtils;
import org.springframework.util.StringUtils;

import javax.validation.constraints.NotBlank;
import java.util.Map;
import java.util.function.Function;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PropertyMetric {
    @Schema(description = "指标ID")
    @NotBlank
    private String id;

    @Schema(description = "名称")
    @NotBlank
    private String name;

    @Schema(description = "值,范围值使用逗号分隔")
    private Object value;

    @Schema(description = "是否为范围值")
    private boolean range;

    @Schema(description = "其他拓展配置")
    private Map<String, Object> expands;

    public Object castValue() {
        if (value == null) {
            return null;
        }
        if (range) {
            return ConverterUtils.tryConvertToList(value, Function.identity());
        }
        return value;
    }

    public PropertyMetric merge(PropertyMetric another) {
        if (!StringUtils.hasText(this.name)) {
            this.setValue(another.value);
        }
        return this;
    }

    public static PropertyMetric of(String id, String name, Object value) {
        PropertyMetric metric = new PropertyMetric();
        metric.setId(id);
        metric.setName(name);
        metric.setValue(value);
        return metric;
    }

    public static PropertyMetric of(Object mapMetric) {
        return FastBeanCopier.copy(mapMetric, new PropertyMetric());
    }
}

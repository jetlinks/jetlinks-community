package org.jetlinks.community.things.data;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hswebframework.web.validator.ValidatorUtils;
import org.jetlinks.community.timeseries.query.Aggregation;
import org.springframework.util.StringUtils;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class PropertyAggregation {
    @Schema(description = "属性ID")
    @NotBlank
    private String property; //要聚合对字段

    @Schema(description = "别名,默认和property一致")
    private String alias; //别名

    @Schema(description = "聚合方式,支持(count,sum,max,min,avg)", type = "string")
    @NotNull
    private Aggregation agg; //聚合函数

    public String getAlias() {
        if (StringUtils.isEmpty(alias)) {
            return property;
        }
        return alias;
    }

    public void validate() {
        ValidatorUtils.tryValidate(this);
    }
}

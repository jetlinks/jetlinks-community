package org.jetlinks.community.timeseries.query;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.util.StringUtils;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;

@Getter
@Setter
@AllArgsConstructor
public class AggregationColumn {

    @Schema(description = "列名")
    @NotBlank
    private String property;

    @Schema(description = "别名")
    private String alias;

    @Schema(description = "聚合方式,支持(min,max,avg,sum,count)", type = "string")
    @NotNull
    private Aggregation aggregation;

    private Object defaultValue;

    public AggregationColumn(@NotBlank String property, String alias, @NotNull Aggregation aggregation) {
        this.property = property;
        this.alias = alias;
        this.aggregation = aggregation;
    }

    public AggregationColumn() {
    }

    public Object getDefaultValue() {
        if (defaultValue != null) {
            return defaultValue;
        }
        if (aggregation != null) {
            return defaultValue = aggregation.getDefaultValue();
        }
        return null;
    }

    public String getAlias() {
        if (StringUtils.hasText(alias)) {
            return alias;
        }
        return property;
    }

}

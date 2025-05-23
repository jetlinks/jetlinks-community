/*
 * Copyright 2025 JetLinks https://www.jetlinks.cn
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.jetlinks.community.timeseries.query;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.util.StringUtils;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

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

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
package org.jetlinks.community.things.data;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.apache.commons.lang3.StringUtils;
import org.hswebframework.web.validator.ValidatorUtils;
import org.jetlinks.community.timeseries.query.Aggregation;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

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

    @Schema(description = "聚合默认值")
    private Object defaultValue;//默认值

    public PropertyAggregation(String property, String alias, Aggregation agg) {
        this(property, alias, agg, null);
    }

    public Object getDefaultValue() {
        if (defaultValue != null) {
            return defaultValue;
        }
        if (agg != null) {
            return defaultValue = agg.getDefaultValue();
        }
        return null;
    }

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

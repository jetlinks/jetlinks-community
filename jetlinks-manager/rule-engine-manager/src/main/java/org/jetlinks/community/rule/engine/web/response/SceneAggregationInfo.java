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
package org.jetlinks.community.rule.engine.web.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;
import org.hswebframework.web.i18n.LocaleUtils;
import org.jetlinks.community.reactorql.aggregation.AggregationSupport;

import java.util.Locale;

/**
 * 聚合函数类型.
 *
 * @author zhangji 2025/1/10
 * @since 2.3
 */
@Getter
@Setter
public class SceneAggregationInfo {

    @Schema(description = "函数")
    private String id;

    @Schema(description = "名称")
    private String name;

    public static SceneAggregationInfo of(AggregationSupport support, Locale locale) {
        String id = support.getId();
        SceneAggregationInfo aggregationInfo = new SceneAggregationInfo();
        aggregationInfo.setId(id);
        aggregationInfo.setName(LocaleUtils
                                .resolveMessage("message.scene_aggregation_name_" + id,
                                                locale,
                                                support.getName()));
        return aggregationInfo;
    }

}
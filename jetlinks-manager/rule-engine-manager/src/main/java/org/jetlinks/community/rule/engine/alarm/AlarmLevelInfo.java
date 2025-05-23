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
package org.jetlinks.community.rule.engine.alarm;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;
import org.hswebframework.ezorm.rdb.mapping.annotation.JsonCodec;
import org.hswebframework.web.i18n.SingleI18nSupportEntity;

import java.util.Map;

/**
 * @author bestfeng
 */
@Getter
@Setter
public class AlarmLevelInfo implements SingleI18nSupportEntity {

    @Schema(description = "级别")
    private Integer level;

    @Schema(description = "名称")
    private String title;

    @JsonCodec
    @Schema(description = "国际化信息")
    private Map<String, String> i18nMessages;

    public String getTitle() {
        return getI18nMessage("title", title);
    }
}

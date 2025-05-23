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
import org.jetlinks.community.rule.engine.executor.device.DeviceSelectorProvider;

import java.io.Serializable;

/**
 * @author liusq
 * @date 2024/4/17
 */
@Getter
@Setter
public class SelectorInfo implements Serializable {
    @Schema(description = "ID")
    private String id;

    @Schema(description = "名称")
    private String name;

    @Schema(description = "说明")
    private String description;

    public static SelectorInfo of(DeviceSelectorProvider provider) {
        SelectorInfo info = new SelectorInfo();
        info.setId(provider.getProvider());

        info.setName(LocaleUtils
                         .resolveMessage("message.device_selector_" + provider.getProvider(), provider.getName()));

        info.setDescription(LocaleUtils
                                .resolveMessage("message.device_selector_" + provider.getProvider() + "_desc", provider.getName()));
        return info;
    }
}

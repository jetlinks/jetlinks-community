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
package org.jetlinks.community.network.manager.web.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;
import org.hswebframework.web.i18n.LocaleUtils;
import org.jetlinks.community.gateway.supports.DeviceGatewayProvider;

import java.util.Locale;

@Getter
@Setter
public class DeviceGatewayProviderInfo {
    @Schema(description = "接入方式ID")
    private String id;

    @Schema(description = "接入方式名称")
    private String name;

    @Schema(description = "接入方式说明")
    private String description;

    @Schema(description = "接入通道")
    private String channel;

    public static DeviceGatewayProviderInfo of(DeviceGatewayProvider provider, Locale locale) {
        DeviceGatewayProviderInfo info = new DeviceGatewayProviderInfo();
        info.setId(provider.getId());
        info.setChannel(provider.getChannel());
        info.setName(LocaleUtils
                         .resolveMessage("device.gateway.provider." + provider.getId() + ".name",
                                         locale,
                                         provider.getName()));

        info.setDescription(LocaleUtils
                                .resolveMessage("device.gateway.provider." + provider.getId() + ".description",
                                                locale,
                                                provider.getDescription()));
        return info;
    }
}

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
package org.jetlinks.community.plugin.utils;

import org.jetlinks.core.device.DeviceOperator;
import org.jetlinks.core.device.DeviceProductOperator;
import org.jetlinks.core.message.*;
import org.jetlinks.plugin.core.Plugin;
import org.jetlinks.plugin.internal.PluginDataIdMapper;
import org.jetlinks.community.plugin.device.ExternalDeviceOperator;
import org.jetlinks.community.plugin.device.ExternalDeviceProductOperator;
import org.springframework.util.Assert;
import reactor.core.publisher.Mono;
import reactor.function.Function3;

public class PluginUtils {


    public static Mono<DeviceProductOperator> transformToExternalProduct(PluginDataIdMapper idMapper,
                                                                         Plugin plugin,
                                                                         DeviceProductOperator product) {
        return idMapper
            .getExternalId(PluginDataIdMapper.TYPE_PRODUCT, plugin.getId(), product.getId())
            .map(ext -> new ExternalDeviceProductOperator(ext, product));
    }

    public static Mono<DeviceOperator> transformToExternalDevice(PluginDataIdMapper idMapper,
                                                                 Plugin plugin,
                                                                 DeviceOperator device) {
        return idMapper
            .getExternalId(PluginDataIdMapper.TYPE_DEVICE, plugin.getId(), device.getDeviceId())
            .map(ext -> new ExternalDeviceOperator(ext, plugin.getId(), idMapper, device));
    }

    public static <T extends ThingMessage> Mono<T> transformToInternalMessage(PluginDataIdMapper idMapper,
                                                                              Plugin plugin,
                                                                              T message) {
        return transformMessage(plugin, message, idMapper::getInternalId);
    }

    public static <T extends ThingMessage> Mono<T> transformToExternalMessage(PluginDataIdMapper idMapper,
                                                                              Plugin plugin,
                                                                              T message) {
        return transformMessage(plugin, message, idMapper::getExternalId);
    }

    private static <T extends ThingMessage> Mono<T> transformMessage(Plugin plugin,
                                                                     T message,
                                                                     Function3<String, String, String, Mono<String>> mapper) {

        Assert.hasText(message.getThingId(),
                       () -> message.getThingType() + "Id must not be empty");

        DeviceMessage child = null;

        if (message instanceof ChildDeviceMessage) {
            Message msg = ((ChildDeviceMessage) message).getChildDeviceMessage();
            if (msg instanceof DeviceMessage) {
                child = ((DeviceMessage) msg);
            }
        } else if (message instanceof ChildDeviceMessageReply) {
            Message msg = ((ChildDeviceMessageReply) message).getChildDeviceMessage();
            if (msg instanceof DeviceMessage) {
                child = ((DeviceMessage) msg);
            }
        }

        if (child != null) {
            return transform0(plugin, child, mapper)
                .then(transform0(plugin, message, mapper));
        }
        return transform0(plugin, message, mapper);
    }

    private static <T extends ThingMessage> Mono<T> transform0(Plugin plugin,
                                                               T message,
                                                               Function3<String, String, String, Mono<String>> mapper) {
        return mapper
            .apply(message.getThingType(), plugin.getId(), message.getThingId())
            .map(internalId -> {
                message.thingId(message.getThingType(), internalId);
                return message;
            });
    }
}

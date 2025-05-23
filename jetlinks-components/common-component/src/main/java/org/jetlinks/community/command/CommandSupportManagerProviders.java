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
package org.jetlinks.community.command;

import org.jetlinks.core.command.CommandSupport;
import reactor.core.publisher.Mono;

import java.util.Collections;
import java.util.Map;
import java.util.Optional;

/**
 * 命令支持管理提供商工具类,用于提供对{@link CommandSupportManagerProvider}相关通用操作.
 *
 * @author zhouhao
 * @see CommandSupportManagerProvider
 * @see CommandSupportManagerProviders#getCommandSupport(String, Map)
 * @since 2.2
 */
public class CommandSupportManagerProviders {


    /**
     * 根据服务ID获取CommandSupport.
     * <pre>{@code
     *
     *  CommandSupportManagerProviders
     *      .getCommandSupport("deviceService:device",Collections.emptyMap())
     *
     * }</pre>
     *
     * @param serviceId serviceId 服务名
     * @return CommandSupport
     * @see InternalSdkServices
     * @see org.jetlinks.sdk.server.SdkServices
     */
    public static Mono<CommandSupport> getCommandSupport(String serviceId) {
        return getCommandSupport(serviceId, Collections.emptyMap());
    }

    /**
     * 根据服务ID和支持ID获取CommandSupport.
     *
     * @param serviceId 服务ID
     * @param supportId 支持ID
     * @return CommandSupport
     */
    public static Mono<CommandSupport> getCommandSupport(String serviceId, String supportId) {
        return getProviderNow(serviceId)
            .getCommandSupport(supportId, Collections.emptyMap())
            .cast(CommandSupport.class);
    }

    /**
     * 根据服务ID获取CommandSupport.
     * <pre>{@code
     *
     *  CommandSupportManagerProviders
     *      .getCommandSupport("deviceService:device",Collections.emptyMap())
     *
     * }</pre>
     *
     * @param serviceId serviceId 服务名
     * @param options   options
     * @return CommandSupport
     * @see InternalSdkServices
     * @see org.jetlinks.sdk.server.SdkServices
     */
    public static Mono<CommandSupport> getCommandSupport(String serviceId,
                                                         Map<String, Object> options) {
        //fast path
        CommandSupportManagerProvider provider = CommandSupportManagerProvider
            .supports
            .get(serviceId)
            .orElse(null);
        if (provider != null) {
            return provider
                .getCommandSupport(serviceId, options)
                .cast(CommandSupport.class);
        }
        String supportId = serviceId;
        // deviceService:product
        if (serviceId.contains(":")) {
            String[] arr = serviceId.split(":", 2);
            serviceId = arr[0];
            supportId = arr[1];
        }
        String finalServiceId = serviceId;
        String finalSupportId = supportId;
        return Mono.defer(() -> getProviderNow(finalServiceId).getCommandSupport(finalSupportId, options));
    }

    /**
     * 注册命令支持
     *
     * @param provider {@link CommandSupportManagerProvider#getProvider()}
     */
    public static void register(CommandSupportManagerProvider provider) {
        CommandSupportManagerProvider.supports.register(provider.getProvider(), provider);
    }

    /**
     * 获取命令支持
     *
     * @param provider {@link CommandSupportManagerProvider#getProvider()}
     * @return Optional
     */
    public static Optional<CommandSupportManagerProvider> getProvider(String provider) {
        return CommandSupportManagerProvider.supports.get(provider);
    }

    /**
     * 获取命令支持,如果不存在则抛出异常{@link UnsupportedOperationException}
     *
     * @param provider provider {@link CommandSupportManagerProvider#getProvider()}
     * @return CommandSupportManagerProvider
     */
    public static CommandSupportManagerProvider getProviderNow(String provider) {
        return CommandSupportManagerProvider.supports.getNow(provider);
    }


}

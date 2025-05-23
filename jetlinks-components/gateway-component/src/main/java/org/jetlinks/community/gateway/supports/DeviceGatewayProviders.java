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
package org.jetlinks.community.gateway.supports;

import org.hswebframework.web.exception.I18nSupportException;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

public class DeviceGatewayProviders {
    private static final Map<String, DeviceGatewayProvider> providers = new ConcurrentHashMap<>();

    public static void register(DeviceGatewayProvider provider) {
        providers.put(provider.getId(), provider);
    }

    public static Optional<DeviceGatewayProvider> getProvider(String provider) {
        return Optional.ofNullable(providers.get(provider));
    }

    public static DeviceGatewayProvider getProviderNow(String provider) {
        DeviceGatewayProvider gatewayProvider = providers.get(provider);
        if (null == gatewayProvider) {
            throw new I18nSupportException("error.unsupported_device_gateway_provider", provider);
        }
        return gatewayProvider;
    }

    public static List<DeviceGatewayProvider> getAll() {
        return new ArrayList<>(providers.values());
    }

}

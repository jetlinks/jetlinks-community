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

package org.jetlinks.community.device.message.transparent;

import org.hswebframework.web.exception.I18nSupportException;
import org.jctools.maps.NonBlockingHashMap;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class TransparentMessageCodecProviders {

    public static Map<String, TransparentMessageCodecProvider> providers = new NonBlockingHashMap<>();


    static void addProvider(TransparentMessageCodecProvider provider) {
        providers.put(provider.getProvider(), provider);
    }

    public static List<TransparentMessageCodecProvider> getProviders() {
        return new ArrayList<>(providers.values());
    }

    public static Optional<TransparentMessageCodecProvider> getProvider(String provider) {
        return Optional.ofNullable(providers.get(provider));
    }

    public static TransparentMessageCodecProvider getProviderNow(String provider) {
        return getProvider(provider)
            .orElseThrow(()->new I18nSupportException("error.unsupported_codec",provider));
    }
}

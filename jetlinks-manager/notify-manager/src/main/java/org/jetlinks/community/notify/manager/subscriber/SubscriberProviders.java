package org.jetlinks.community.notify.manager.subscriber;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

public class SubscriberProviders {

    private final static Map<String, SubscriberProvider> providers = new ConcurrentHashMap<>();


    public static void register(SubscriberProvider provider) {
        providers.put(provider.getId(), provider);
    }

    public static List<SubscriberProvider> getProviders() {
        return new ArrayList<>(providers.values());
    }

    public static Optional<SubscriberProvider> getProvider(String id) {
        return Optional.ofNullable(providers.get(id));
    }
}

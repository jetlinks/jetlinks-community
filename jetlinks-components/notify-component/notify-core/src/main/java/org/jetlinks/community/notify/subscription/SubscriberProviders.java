package org.jetlinks.community.notify.subscription;

import java.util.List;
import java.util.Optional;

public class SubscriberProviders {


    public static void register(SubscriberProvider provider) {
        SubscriberProvider.supports.register(provider.getId(), provider);
    }

    public static List<SubscriberProvider> getProviders() {
        return SubscriberProvider.supports.getAll();
    }

    public static Optional<SubscriberProvider> getProvider(String id) {
        return SubscriberProvider.supports.get(id);
    }
}

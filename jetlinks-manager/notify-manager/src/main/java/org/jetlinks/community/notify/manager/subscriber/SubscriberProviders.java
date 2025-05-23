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

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

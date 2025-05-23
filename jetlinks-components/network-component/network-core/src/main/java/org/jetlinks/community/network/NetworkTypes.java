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
package org.jetlinks.community.network;


import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class NetworkTypes {

    private static final Map<String, NetworkType> all = new ConcurrentHashMap<>();

    public static void register(Collection<NetworkType> transport) {
        transport.forEach(NetworkTypes::register);
    }

    public static void register(NetworkType transport) {
        all.put(transport.getId().toUpperCase(), transport);
    }

    public static List<NetworkType> get() {
        return new ArrayList<>(all.values());
    }

    public static Optional<NetworkType> lookup(String id) {
        return Optional.ofNullable(all.get(id.toUpperCase()));
    }

}

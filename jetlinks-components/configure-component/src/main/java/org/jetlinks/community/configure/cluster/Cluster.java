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
package org.jetlinks.community.configure.cluster;

import org.apache.commons.collections4.MapUtils;
import org.springframework.core.env.Environment;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public class Cluster {

    public static final String TAG_NAME = "name";
    public static final String TAG_ID = "id";
    private static String ID = "default";
    private static String SAFE_ID = "default";

    private static String NAME = "default";
    private static Map<String, String> TAGS = Collections.emptyMap();

    public static String id() {
        return ID;
    }

    public static String name() {
        return NAME;
    }

    public static String safeId() {
        return SAFE_ID;
    }

    public static Map<String, String> tags() {
        return TAGS;
    }

    public static boolean hasTag(Map<String, String> tag) {
        if (MapUtils.isEmpty(tag)) {
            return false;
        }
        for (Map.Entry<String, String> entry : tag.entrySet()) {
            if (!Objects.equals(TAGS.get(entry.getKey()), entry.getValue())) {
                return false;
            }
        }
        return true;
    }

    public static synchronized void setup(Environment env){
        Map<String, String> _tags = new HashMap<>(TAGS);
        _tags.put("port",env.getProperty("server.port"));

    }

    public static synchronized void setup(String id, String name, Map<String, String> tags) {
        ID = id;
        SAFE_ID = ID.replaceAll("[\\s\\\\/:*?\"<>|]", "_");

        NAME = name;
        Map<String, String> _tags = new HashMap<>(tags);
        _tags.putIfAbsent(TAG_NAME, name);
        _tags.putIfAbsent(TAG_ID, id);
        TAGS = Collections.unmodifiableMap(_tags);
    }

}

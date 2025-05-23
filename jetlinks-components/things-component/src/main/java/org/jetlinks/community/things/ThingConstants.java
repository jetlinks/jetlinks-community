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
package org.jetlinks.community.things;

import org.jetlinks.core.message.HeaderKey;
import org.jetlinks.core.things.ThingType;
import org.jetlinks.community.PropertyConstants;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public interface ThingConstants {

    HeaderKey<String> templateId = PropertyConstants.Key.of("templateId");

    HeaderKey<Map<String, Object>> contextVar = PropertyConstants.Key.of("_var", ConcurrentHashMap::new, ConcurrentHashMap.class);

    HeaderKey<Set<String>> connectorTrace = PropertyConstants.Key.of("_connectors",
                                                                     ConcurrentHashMap::newKeySet,
                                                                     ConcurrentHashMap.KeySetView.class);

    interface Topics {

        static String properties() {
            return "/thing/*/property/*";
        }

        static String[] properties(ThingType thingType, String thingId) {

            return new String[]{
                thingType.getTopicPrefix("*", thingId) + "/message/property/report",
                thingType.getTopicPrefix("*", thingId) + "/message/property/read/reply",
                thingType.getTopicPrefix("*", thingId) + "/message/property/write/reply",

            };
        }


    }


}

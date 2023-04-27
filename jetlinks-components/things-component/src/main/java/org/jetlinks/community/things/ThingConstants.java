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

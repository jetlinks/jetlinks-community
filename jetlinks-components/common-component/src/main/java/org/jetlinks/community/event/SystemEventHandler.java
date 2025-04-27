package org.jetlinks.community.event;

import org.jetlinks.core.utils.StringBuilderUtils;

public interface SystemEventHandler {

    static String topic(String operationType, String operationId, String level) {
        return StringBuilderUtils
            .buildString(operationType, operationId, level, (a, b, c, builder) -> {
                //     /sys-event/{operationType}/{operationId}/{level}
                builder.append("/sys-event/")
                       .append(a)
                       .append('/')
                       .append(b)
                       .append('/')
                       .append(c);
            });
    }

    void handle(SystemEvent event);

}

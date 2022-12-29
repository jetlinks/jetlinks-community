package org.jetlinks.community.rule.engine.service;

import lombok.*;

import java.io.Serializable;
import java.util.Date;


@Getter
@Setter
@AllArgsConstructor(staticName = "of")
@NoArgsConstructor
@Generated
public class DebugMessage implements Serializable {

    private String type;

    private String contextId;

    private Object message;

    private Date timestamp;

    public static DebugMessage of(String type, String contextId, Object message) {
        return of(type, contextId, message, new Date());
    }
}

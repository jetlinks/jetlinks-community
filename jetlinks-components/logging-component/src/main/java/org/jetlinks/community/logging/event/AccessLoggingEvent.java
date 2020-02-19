package org.jetlinks.community.logging.event;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import org.jetlinks.community.logging.access.SerializableAccessLog;

@Getter
@Setter
@AllArgsConstructor
public class AccessLoggingEvent {
    SerializableAccessLog log;
}

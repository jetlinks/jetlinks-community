package org.jetlinks.community.logging.event;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import org.jetlinks.community.logging.system.SerializableSystemLog;

@Getter
@Setter
@AllArgsConstructor
public class SystemLoggingEvent {
    SerializableSystemLog log;
}

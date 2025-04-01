package org.jetlinks.community.auth.event;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.hswebframework.web.event.DefaultAsyncEvent;

@AllArgsConstructor
@Getter
public class MenuGrantEvent extends DefaultAsyncEvent {
    private String targetType;
    private String targetId;
}

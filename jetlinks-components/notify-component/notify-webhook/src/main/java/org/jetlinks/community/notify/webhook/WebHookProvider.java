package org.jetlinks.community.notify.webhook;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.jetlinks.community.notify.Provider;

@Getter
@AllArgsConstructor
public enum WebHookProvider implements Provider {

    http("HTTP")
    ;

    private final String name;

    @Override
    public String getId() {
        return name();
    }
}


package org.jetlinks.community.notify.voice;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.jetlinks.community.notify.Provider;

@Getter
@AllArgsConstructor
public enum VoiceProvider implements Provider {

    aliyun("阿里云")
    ;

    private final String name;

    @Override
    public String getId() {
        return name();
    }
}

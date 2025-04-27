package org.jetlinks.community.auth.cipher;

import lombok.Getter;
import lombok.Setter;

import java.time.Duration;

@Getter
@Setter
public class CipherProperties {

    private boolean enabled;

    private Duration keyTtl = Duration.ofMinutes(5);

}

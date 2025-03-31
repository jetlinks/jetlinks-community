package org.jetlinks.community.auth.cipher;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CipherConfig {

    private boolean enabled;

    private String publicKey;

    private String id;
}

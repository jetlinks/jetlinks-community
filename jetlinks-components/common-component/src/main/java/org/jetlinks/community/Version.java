package org.jetlinks.community;

import lombok.Getter;

@Getter
public class Version {
    public static Version current = new Version();

    private final String edition = "community";

    private final String version = "1.7.0-SNAPSHOT";

}

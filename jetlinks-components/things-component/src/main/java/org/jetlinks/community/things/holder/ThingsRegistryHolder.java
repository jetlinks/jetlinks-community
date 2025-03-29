package org.jetlinks.community.things.holder;

import org.jetlinks.core.things.ThingsRegistry;

public class ThingsRegistryHolder {

    static ThingsRegistry REGISTRY;

    public static ThingsRegistry registry() {
        if (REGISTRY == null) {
            throw new IllegalStateException("registry not initialized");
        }
        return REGISTRY;
    }
}

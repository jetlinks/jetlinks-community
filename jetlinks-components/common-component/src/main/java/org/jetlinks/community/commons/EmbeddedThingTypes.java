package org.jetlinks.community.commons;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.jetlinks.core.things.ThingType;
import org.jetlinks.core.things.ThingTypes;

@Getter
@AllArgsConstructor
public enum EmbeddedThingTypes implements ThingType {

    organization("组织"),
    user("用户");

    static {
        for (EmbeddedThingTypes value : EmbeddedThingTypes.values()) {
            ThingTypes.register(value);
        }
    }

    private final String name;

    @Override
    public String getId() {
        return name();
    }
}

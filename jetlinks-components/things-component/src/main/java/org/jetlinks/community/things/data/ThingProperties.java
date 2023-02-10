package org.jetlinks.community.things.data;

import lombok.Getter;

import java.util.HashMap;
import java.util.Map;

@Getter
public class ThingProperties extends HashMap<String, Object> {

    private final String thingId;


    public ThingProperties(Map<String, Object> data,String thingIdProperty) {
        super(data);
        this.thingId = (String) data.get(thingIdProperty);
    }

}

package org.jetlinks.community.relation.impl;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * 数据
 *
 * @author gyl
 * @since 2.2
 */
public class ObjectData extends HashMap<String, Object> {
    private static final long serialVersionUID = 362498820763181265L;

    private String id;

    public String getId() {
        if (id == null) {
            id = Optional
                .ofNullable(this.get("id"))
                .map(String::valueOf)
                .orElse(null);
        }
        return id;
    }

    public ObjectData(Map<String, Object> data) {
        super(data);
    }

    public ObjectData() {
    }


}

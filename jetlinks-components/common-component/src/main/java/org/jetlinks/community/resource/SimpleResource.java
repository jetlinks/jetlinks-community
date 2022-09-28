package org.jetlinks.community.resource;

import com.alibaba.fastjson.JSON;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.lang.reflect.Type;

@AllArgsConstructor(staticName = "of")
@NoArgsConstructor
@Getter
@Setter
public class SimpleResource implements Resource {
    private String id;
    private String type;
    private String resource;

    @Override
    public <T> T as(Class<T> type) {
        return JSON.parseObject(resource, type);
    }

    @Override
    public <T> T as(Type type) {
        return JSON.parseObject(resource, type);
    }

    @Override
    public String asString() {
        return resource;
    }
}

package org.jetlinks.community.resource;

import java.lang.reflect.Type;

public interface Resource {

    String getId();

    String getType();

    <T> T as(Class<T> type);

    <T> T as(Type type);

    String asString();
}

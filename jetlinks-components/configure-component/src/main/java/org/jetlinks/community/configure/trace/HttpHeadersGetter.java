package org.jetlinks.community.configure.trace;

import io.opentelemetry.context.propagation.TextMapGetter;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.util.MultiValueMap;

import javax.annotation.Nullable;
import java.util.List;

public class HttpHeadersGetter implements TextMapGetter<MultiValueMap<String, String>> {
    public static final HttpHeadersGetter INSTANCE = new HttpHeadersGetter();


    @Override
    public Iterable<String> keys(MultiValueMap<String, String> map) {
        return map.keySet();
    }

    @Nullable
    @Override
    public String get(@Nullable MultiValueMap<String, String> map, String key) {
        if (map == null) {
            return null;
        }
        List<String> code = map.get(key);

        return CollectionUtils.isEmpty(code) ? null : code.get(code.size() - 1);
    }
}

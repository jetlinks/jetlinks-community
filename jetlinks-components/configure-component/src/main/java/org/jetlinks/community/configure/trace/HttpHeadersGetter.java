/*
 * Copyright 2025 JetLinks https://www.jetlinks.cn
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
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

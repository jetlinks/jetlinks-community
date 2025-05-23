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

import org.springframework.http.server.reactive.ServerHttpRequest;
import reactor.function.Consumer3;

public class HttpServerHeaderTraceWriter implements Consumer3<ServerHttpRequest.Builder, String, String> {

    public static final HttpServerHeaderTraceWriter INSTANCE = new HttpServerHeaderTraceWriter();

    @Override
    public void accept(ServerHttpRequest.Builder builder, String key, String value) {
        builder.headers(headers -> HttpHeaderTraceWriter.INSTANCE.accept(headers, key, value));
    }
}

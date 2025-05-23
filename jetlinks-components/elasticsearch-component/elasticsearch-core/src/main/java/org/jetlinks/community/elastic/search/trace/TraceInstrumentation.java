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
package org.jetlinks.community.elastic.search.trace;

import co.elastic.clients.elasticsearch.core.SearchRequest;
import co.elastic.clients.transport.Endpoint;
import co.elastic.clients.transport.TransportOptions;
import co.elastic.clients.transport.http.TransportHttpClient;
import co.elastic.clients.transport.instrumentation.Instrumentation;
import co.elastic.clients.transport.instrumentation.NoopInstrumentation;
import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.StatusCode;
import io.opentelemetry.context.Scope;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jetlinks.core.lang.SeparatedCharSequence;
import org.jetlinks.core.lang.SharedPathString;
import org.jetlinks.core.message.codec.http.HttpUtils;
import org.jetlinks.core.trace.TraceHolder;

import java.net.URI;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

@Slf4j
public class TraceInstrumentation implements Instrumentation {
    private static final Set<String> SEARCH_ENDPOINTS = new HashSet<>(Arrays.asList(
        "render_search_template",
        "terms_enum",
        "msearch_template",
        "eql.search",
        "msearch",
        "search_template",
        "async_search.submit",
        "search"
    ));

    private static final AttributeKey<String> ATTR_HTTP_REQUEST_METHOD = AttributeKey.stringKey("http.request.method");
    private static final AttributeKey<String> ATTR_REQUEST_URI = AttributeKey.stringKey("http.request.uri");
    private static final AttributeKey<String> ATTR_REQUEST_BODY = AttributeKey.stringKey("http.request.body");

    private final static SharedPathString spanNameTemplate =
        SharedPathString.of("/_elastic/*");

    @Override
    public <TRequest> Context newContext(TRequest tRequest, Endpoint<TRequest, ?, ?> endpoint) {
        String endpointId = endpoint.id();
        if (endpointId.startsWith("es/")) {
            endpointId = endpointId.substring(3);
        }
        SeparatedCharSequence spanName = createSpanName(spanNameTemplate.replace(2, endpointId), tRequest);

        if (TraceHolder.isDisabled(spanName)) {
            return NoopInstrumentation.INSTANCE.newContext(tRequest, endpoint);
        }
        Span span = TraceHolder
            .telemetry()
            .getTracer(TraceHolder.appName())
            .spanBuilder(spanName.toString())
            .setParent(io.opentelemetry.context.Context.current())
            .startSpan();

        return new TraceContext(span, endpointId);
    }

    private SeparatedCharSequence createSpanName(SeparatedCharSequence prefix,Object request){
        if(request instanceof SearchRequest req){
           return prefix.append(String.join(",", req.index()));
        }
        return prefix;
    }

    @RequiredArgsConstructor
    static class TraceContext implements Context {
        final Span span;
        final String endpointId;

        String pathAndQuery;

        @Override
        @SuppressWarnings("all")
        public ThreadScope makeCurrent() {
            Scope scope = span.makeCurrent();
            return scope::close;
        }

        @Override
        public void beforeSendingHttpRequest(TransportHttpClient.Request httpRequest, TransportOptions options) {

            pathAndQuery = HttpUtils.appendUrlParameter(httpRequest.path(), httpRequest.queryParams());

            span.setAttribute(ATTR_HTTP_REQUEST_METHOD, httpRequest.method());

            Iterable<ByteBuffer> body = httpRequest.body();
            if (span.isRecording() && body != null && SEARCH_ENDPOINTS.contains(endpointId)) {
                StringBuilder sb = new StringBuilder();
                for (ByteBuffer buf : body) {
                    buf.mark();
                    sb.append(StandardCharsets.UTF_8.decode(buf));
                    buf.reset();
                }
                span.setAttribute(ATTR_REQUEST_BODY, sb.toString());
            }
        }

        @Override
        public void afterReceivingHttpResponse(TransportHttpClient.Response httpResponse) {
            try {
                if (span.isRecording()) {
                    URI uri = httpResponse.node().uri();
                    String fullUrl = uri.resolve(pathAndQuery).toString();
                    span.setAttribute(ATTR_REQUEST_URI, fullUrl);
                }
            } catch (RuntimeException e) {
                log.debug("Failed capturing response information for the OpenTelemetry span.", e);
                // ignore
            }
        }

        @Override
        public <TResponse> void afterDecodingApiResponse(TResponse apiResponse) {

        }

        @Override
        public void recordException(Throwable thr) {
            span.setStatus(StatusCode.ERROR, thr.getMessage());
            span.recordException(thr);
        }

        @Override
        public void close() {
            span.end();
        }
    }
}

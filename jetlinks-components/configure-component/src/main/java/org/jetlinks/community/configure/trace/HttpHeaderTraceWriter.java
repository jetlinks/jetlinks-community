package org.jetlinks.community.configure.trace;

import org.springframework.http.HttpHeaders;
import reactor.function.Consumer3;

public class HttpHeaderTraceWriter implements Consumer3<HttpHeaders, String, String> {

    public static final HttpHeaderTraceWriter INSTANCE  =new HttpHeaderTraceWriter();

    @Override
    public void accept(HttpHeaders headers, String key, String value) {
        headers.set(key, value);
    }
}

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
package org.jetlinks.community.network.http;

import com.alibaba.fastjson.JSON;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import lombok.AllArgsConstructor;
import lombok.Generated;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.jetlinks.core.message.codec.http.Header;
import org.jetlinks.core.message.codec.http.HttpRequestMessage;
import org.jetlinks.core.message.codec.http.HttpUtils;
import org.jetlinks.core.message.codec.http.MultiPart;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;

import java.util.*;

/**
 * @author bsetfeng
 * @since 1.0
 **/
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Generated
public class DefaultHttpRequestMessage implements HttpRequestMessage {

    //消息体
    private ByteBuf payload;

    private String url;

    //请求方法
    private HttpMethod method;

    //请求头
    private List<Header> headers = new ArrayList<>();

    //参数
    private Map<String, String> queryParameters = new HashMap<>();

    //请求类型
    private MediaType contentType;

    private MultiPart multiPart;

    @Override
    public Optional<MultiPart> multiPart() {
        return Optional
            .ofNullable(multiPart)
            .filter(part -> part.getParts().size() > 0);
    }

    public void setBody(Object body) {
        if (body instanceof ByteBuf) {
            setPayload(((ByteBuf) body));
        } else if (body instanceof String) {
            setPayload(Unpooled.wrappedBuffer(((String) body).getBytes()));
        } else if (body instanceof byte[]) {
            setPayload(Unpooled.wrappedBuffer(((byte[]) body)));
        } else if (MediaType.APPLICATION_JSON.includes(getContentType())) {
            setPayload(Unpooled.wrappedBuffer(JSON.toJSONBytes(body)));
        } else if (MediaType.APPLICATION_FORM_URLENCODED.includes(getContentType()) && body instanceof Map) {
            setPayload(Unpooled.wrappedBuffer(HttpUtils.createEncodedUrlParams(((Map<?, ?>) body)).getBytes()));
        } else if (body != null) {
            setPayload(Unpooled.wrappedBuffer(JSON.toJSONBytes(body)));
        } else {
            setPayload(Unpooled.EMPTY_BUFFER);
        }
    }

    @Override
    public String toString() {
        return print();
    }
}

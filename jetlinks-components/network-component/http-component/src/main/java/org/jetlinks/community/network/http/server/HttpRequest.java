package org.jetlinks.community.network.http.server;

import io.netty.buffer.ByteBuf;
import org.jetlinks.core.message.codec.http.Header;
import org.jetlinks.core.message.codec.http.HttpRequestMessage;
import org.jetlinks.core.message.codec.http.MultiPart;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.util.MultiValueMap;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.net.InetSocketAddress;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * HTTP请求接口,用于定义HTTP请求
 *
 * @author zhouhao
 * @since 1.0
 */
public interface HttpRequest {

    /**
     * 请求的URL地址,如: http://127.0.0.1/test
     *
     * @return 请求的URL地址
     */
    String getUrl();

    /**
     * 请求路径,如: /test
     *
     * @return 请求路径
     */
    String getPath();

    /**
     * @return 远程客户端IP地址
     */
    String getRemoteIp();

    /**
     * 获取远程客户端真实地址，通过解析HTTP请求头中的: X-Real-IP,X-Forwarded-For,Proxy-Client-IP信息，
     *
     * @return 远程客户端真实地址
     */
    String getRealIp();

    /**
     * 获取远程套接字地址,包含ip和端口信息
     *
     * @return 远程套接字地址
     */
    InetSocketAddress getClientAddress();

    /**
     * @return 请求方法
     */
    HttpMethod getMethod();

    /**
     * @return 请求体类型
     */
    MediaType getContentType();

    /**
     * 根据key获取查询参数(查询参数为url中的参数，如: /query?name=test)
     *
     * @param key 参数key
     * @return 参数值
     */
    Optional<String> getQueryParameter(String key);

    /**
     * 获取全部的查询参数(查询参数为url中的参数，如: /query?name=test)
     *
     * @return 参数值
     */
    Map<String, String> getQueryParameters();

    /**
     * 获取POST请求参数，当contentType为<code>application/x-www-form-urlencoded</code>时，
     * 通过此方法获取参数信息
     *
     * @return 请求参数
     */
    Map<String, String> getRequestParam();

    /**
     * 获取请求体
     *
     * @return 请求体
     */
    Mono<ByteBuf> getBody();

    /**
     * 获取请求头信息
     *
     * @return 请求头列表
     */
    List<Header> getHeaders();

    /**
     * 根据key获取对应当请求头
     *
     * @param key key
     * @return 请求头
     */
    Optional<Header> getHeader(String key);

    /**
     * 将当前对象转为{@link HttpRequestMessage}
     *
     * @return HttpRequestMessage
     */
    Mono<HttpRequestMessage> toMessage();

    Mono<MultiPart> multiPart();
}

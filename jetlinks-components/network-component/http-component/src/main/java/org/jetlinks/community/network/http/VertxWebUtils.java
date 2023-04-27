package org.jetlinks.community.network.http;

import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.net.SocketAddress;
import org.springframework.util.StringUtils;

import java.util.Optional;

/**
 * @author bsetfeng
 * @since 1.0
 **/
public class VertxWebUtils {

    static final String[] ipHeaders = {
        "X-Forwarded-For",
        "X-Real-IP",
        "Proxy-Client-IP",
        "WL-Proxy-Client-IP"
    };

    /**
     * 获取请求客户端的真实ip地址
     *
     * @param request 请求对象
     * @return ip地址
     */
    public static String getIpAddr(HttpServerRequest request) {
        for (String ipHeader : ipHeaders) {
            String ip = request.getHeader(ipHeader);
            if (!StringUtils.isEmpty(ip) && !ip.contains("unknown")) {
                return ip;
            }
        }
        return Optional.ofNullable(request.remoteAddress())
            .map(SocketAddress::host)
            .orElse("unknown");
    }
}

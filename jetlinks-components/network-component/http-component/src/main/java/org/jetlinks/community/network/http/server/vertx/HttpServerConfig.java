package org.jetlinks.community.network.http.server.vertx;

import io.vertx.core.http.HttpServerOptions;
import lombok.*;
import org.jetlinks.community.network.AbstractServerNetworkConfig;
import org.jetlinks.community.network.resource.NetworkTransport;
import org.jetlinks.community.network.AbstractServerNetworkConfig;
import org.jetlinks.community.network.resource.NetworkTransport;
import org.springframework.util.StringUtils;

import java.util.Collections;
import java.util.Map;

/**
 * HTTP服务配置
 *
 * @author bsetfeng
 * @since 1.0
 **/
@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class HttpServerConfig extends AbstractServerNetworkConfig {

    /**
     * 服务实例数量(线程数)
     */
    private int instance = Math.max(4, Runtime.getRuntime().availableProcessors());

    /**
     * 固定响应头信息
     */
    private Map<String, String> httpHeaders;

    public Map<String, String> getHttpHeaders() {
        return nullMapHandle(httpHeaders);
    }

    private Map<String, String> nullMapHandle(Map<String, String> map) {
        return map == null ? Collections.emptyMap() : map;
    }

    @Override
    public NetworkTransport getTransport() {
        return NetworkTransport.TCP;
    }

    @Override
    public String getSchema() {
        return "http";
    }
}

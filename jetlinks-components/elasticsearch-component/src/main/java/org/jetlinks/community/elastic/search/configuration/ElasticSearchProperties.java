package org.jetlinks.community.elastic.search.configuration;

import lombok.Getter;
import lombok.Setter;
import org.apache.commons.collections.CollectionUtils;
import org.apache.http.HttpHost;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.impl.nio.client.HttpAsyncClientBuilder;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.List;

@ConfigurationProperties(prefix = "elasticsearch.client")

@Getter
@Setter
public class ElasticSearchProperties {

    private String host = "localhost";
    private int port = 9200;

    private int connectionRequestTimeout = 5000;
    private int connectTimeout = 2000;
    private int socketTimeout = 2000;
    private int maxConnTotal = 30;
    private List<String> hosts;


    public HttpHost[] createHosts() {
        if (CollectionUtils.isEmpty(hosts)) {
            return new HttpHost[]{new HttpHost(host, port, "http")};
        }

        return hosts.stream().map(HttpHost::create).toArray(HttpHost[]::new);
    }

    public RequestConfig.Builder applyRequestConfigBuilder(RequestConfig.Builder builder) {

        builder.setConnectTimeout(connectTimeout);
        builder.setConnectionRequestTimeout(connectionRequestTimeout);
        builder.setSocketTimeout(socketTimeout);

        return builder;
    }

    public HttpAsyncClientBuilder applyHttpAsyncClientBuilder(HttpAsyncClientBuilder builder) {
        builder.setMaxConnTotal(maxConnTotal);

        return builder;
    }
}

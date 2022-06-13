package org.jetlinks.community.io.file;

import lombok.Getter;
import lombok.Setter;
import org.hswebframework.web.exception.NotFoundException;
import org.hswebframework.web.utils.DigestUtils;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.util.unit.DataSize;
import org.springframework.web.reactive.function.client.ClientRequest;
import org.springframework.web.reactive.function.client.ExchangeFilterFunction;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;

@Getter
@Setter
@ConfigurationProperties("file.manager")
public class FileProperties {

    public static final String clusterKeyHeader = "cluster-key";

    public static final String serverNodeIdAttr = "server-node-id";

    private String clusterKey = DigestUtils.md5Hex("_JetLinks_FM_K");

    private String storageBasePath = "./data/files";

    private int readBufferSize = (int) DataSize.ofKilobytes(64).toBytes();

    private String serverNodeId = "default";

    /**
     * server1: 192.168.33.222:3322
     */
    private Map<String, String> clusterRute = new HashMap<>();


    public String selectServerNode() {
        int size = clusterRute.size();
        if (size == 0) {
            throw new NotFoundException("error.server_node_notfound");
        }
        return new ArrayList<>(clusterRute.keySet())
            .get(ThreadLocalRandom.current().nextInt(size));
    }

    public ExchangeFilterFunction createWebClientRute() {
        return (clientRequest, exchangeFunction) -> {
            String target = clientRequest
                .attribute(serverNodeIdAttr)
                .map(String::valueOf)
                .map(clusterRute::get)
                .orElseThrow(() -> new NotFoundException("error.server_node_notfound"));
            int idx = target.lastIndexOf(":");
            String host = target.substring(0, idx).trim();
            String port = target.substring(idx + 1).trim();
            return exchangeFunction
                .exchange(
                    ClientRequest
                        .from(clientRequest)
                        .header(clusterKeyHeader, clusterKey)
                        .url(UriComponentsBuilder
                                 .fromUri(clientRequest.url())
                                 .host(host)
                                 .port(port)
                                 .build()
                                 .toUri())
                        .build()
                );
        };
    }


}

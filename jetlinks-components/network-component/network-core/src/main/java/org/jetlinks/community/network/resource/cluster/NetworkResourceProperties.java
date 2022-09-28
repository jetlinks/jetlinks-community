package org.jetlinks.community.network.resource.cluster;


import lombok.Getter;
import lombok.Setter;
import org.jetlinks.community.network.resource.NetworkResource;
import org.jetlinks.community.network.resource.NetworkResource;
import org.jetlinks.community.network.resource.NetworkTransport;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.*;

/**
 * <pre>
 * network:
 *   resources:
 *      - 0.0.0.0:8080-8082/tcp
 *      - 127.0.0.1:8080-8082/udp
 *
 * </pre>
 */
@Getter
@Setter
@ConfigurationProperties(prefix = "network")
public class NetworkResourceProperties {

    private List<String> resources = new ArrayList<>();

    public List<NetworkResource> parseResources() {
        Map<String, NetworkResource> info = new LinkedHashMap<>();

        for (String resource : resources) {
            NetworkTransport protocol = null;
            if (resource.contains("/")) {
                protocol = NetworkTransport.valueOf(resource
                                                       .substring(resource.indexOf("/") + 1)
                                                       .toUpperCase(Locale.ROOT));
                resource = resource.substring(0, resource.indexOf("/"));
            }
            String[] hostAndPort = resource.split(":");
            //只指定端口
            if (hostAndPort.length == 1) {
                hostAndPort = Arrays.copyOf(hostAndPort, 2);
                hostAndPort[1] = hostAndPort[0];
                hostAndPort[0] = "0.0.0.0";
            }
            String host = hostAndPort[0];
            String port = hostAndPort[1];
            NetworkResource res = info.computeIfAbsent(host, hst -> new NetworkResource());
            res.setHost(host);
            //未指定时则同时支持UDP和TCP
            if (protocol == null) {
                List<Integer> ports = getPorts(port);
                res.addPorts(NetworkTransport.UDP, ports);
                res.addPorts(NetworkTransport.TCP, ports);
            } else {
                res.addPorts(protocol, getPorts(port));
            }

        }
        return new ArrayList<>(info.values());

    }

    private List<Integer> getPorts(String port) {
        String[] ports = port.split("-");
        if (ports.length == 1) {
            return Collections.singletonList(Integer.parseInt(ports[0]));
        }
        int startWith = Integer.parseInt(ports[0]);
        int endWith = Integer.parseInt(ports[1]);
        if (startWith > endWith) {
            int temp = startWith;
            startWith = endWith;
            endWith = temp;
        }
        List<Integer> arr = new ArrayList<>(endWith - startWith);
        for (int i = startWith; i <= endWith; i++) {
            arr.add(i);
        }
        return arr;
    }

}

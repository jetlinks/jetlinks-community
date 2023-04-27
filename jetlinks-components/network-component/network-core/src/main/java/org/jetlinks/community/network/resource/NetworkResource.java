package org.jetlinks.community.network.resource;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.jetlinks.core.cluster.ServerNode;

import java.io.Serializable;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 网络资源信息,网卡,可用端口
 *
 * @author zhouhao
 * @since 2.0
 */
@Getter
@Setter
public class NetworkResource implements Serializable {
    private static final long serialVersionUID = 1L;

    /**
     * 网卡Host信息,如: 192.168.1.10
     */
    private String host;

    /**
     * 说明
     */
    private String description;

    /**
     * 端口信息,key为协议,如TCP,UDP.
     */
    private Map<NetworkTransport, Set<Integer>> ports = new HashMap<>();

    public boolean hostIsBindAll() {
        return "0.0.0.0".equals(host)
            || "::".equals(host)
            || "0:0:0:0:0:0:0:0".equals(host);
    }

    public List<PortInfo> getPortList() {
        List<PortInfo> portList = new ArrayList<>();
        Map<Integer, List<NetworkTransport>> ports = new HashMap<>();
        for (Map.Entry<NetworkTransport, Set<Integer>> entry : this.ports.entrySet()) {
            for (Integer port : entry.getValue()) {
                ports.computeIfAbsent(port, (ignore) -> new ArrayList<>())
                     .add(entry.getKey());
            }
        }
        ports.forEach(((port, transports) -> portList.add(PortInfo.of(port, transports))));
        portList.sort(Comparator.comparing(PortInfo::getPort));
        return portList;
    }

    public static NetworkResource of(String host, Integer... ports) {
        NetworkResource resource = new NetworkResource();
        resource.setHost(host);
        if (ports != null && ports.length > 0) {
            resource.withPorts(NetworkTransport.TCP, Arrays.asList(ports));
        }
        return resource;
    }

    /**
     * 判断是否为相同的host
     *
     * @param host Host
     * @return 是否相同
     */
    public boolean isSameHost(String host) {
        //更好的相同host判断逻辑?
        return Objects.equals(host, this.host);
    }

    /**
     * 判断当前资源是否包含有指定的网络端口
     *
     * @param protocol 协议
     * @param port     端口号
     * @return 是否包含端口号
     */
    public boolean containsPort(NetworkTransport protocol, int port) {
        return ports != null && ports.getOrDefault(protocol, Collections.emptySet()).contains(port);
    }

    public Set<Integer> getPorts(NetworkTransport protocol) {
        return ports.getOrDefault(protocol, Collections.emptySet());
    }

    public Set<Integer> tcpPorts() {
        return getPorts(NetworkTransport.TCP);
    }

    public Set<Integer> udpPorts() {
        return getPorts(NetworkTransport.UDP);
    }

    public void removePorts(Map<NetworkTransport, ? extends Collection<Integer>> ports) {
        if (ports == null) {
            return;
        }
        ports.forEach(this::removePorts);
    }

    public void removePorts(NetworkTransport protocol, Collection<Integer> ports) {
        if (null != this.ports) {
            this.ports.compute(protocol, (key, old) -> {
                if (old == null) {
                    return null;
                }
                Set<Integer> newPorts = new TreeSet<>(old);
                ports.forEach(newPorts::remove);
                return newPorts;
            });
        }
    }

    public void addPorts(NetworkTransport transport, Collection<Integer> ports) {
        this.ports.computeIfAbsent(transport, ignore -> new TreeSet<>())
                  .addAll(ports);
    }

    public NetworkResource withPorts(NetworkTransport transport, Collection<Integer> ports) {
        addPorts(transport, ports);
        return this;
    }

    public NetworkResource retainPorts(Map<NetworkTransport, ? extends Collection<Integer>> ports) {
        if (ports == null) {
            return this;
        }
        ports.forEach(this::retainPorts);
        return this;
    }

    public void retainPorts(NetworkTransport protocol, Collection<Integer> ports) {
        if (null != this.ports) {
            this.ports.compute(protocol, (key, old) -> {
                if (old == null) {
                    return new TreeSet<>(ports);
                }
                Set<Integer> newPorts = new TreeSet<>(old);
                newPorts.retainAll(ports);
                return newPorts;
            });
        }
    }

    public NetworkResource copy() {
        NetworkResource resource = new NetworkResource();
        resource.setDescription(description);
        resource.setHost(host);
        resource.setPorts(new ConcurrentHashMap<>());
        if (this.ports != null) {
            for (Map.Entry<NetworkTransport, Set<Integer>> entry : this.ports.entrySet()) {
                resource.getPorts().put(entry.getKey(), new TreeSet<>(entry.getValue()));
            }
        }
        return resource;
    }


    @Getter
    @Setter
    @AllArgsConstructor(staticName = "of")
    @NoArgsConstructor
    public static class PortInfo {
        private int port;
        private List<NetworkTransport> transports;
    }

}

package org.jetlinks.community.network;

public interface ClientNetworkConfig extends NetworkConfig {

    String getRemoteHost();

    int getRemotePort();

    default String getRemoteAddress() {
        return getSchema() + "://" + getRemoteHost() + ":" + getRemotePort();
    }
}

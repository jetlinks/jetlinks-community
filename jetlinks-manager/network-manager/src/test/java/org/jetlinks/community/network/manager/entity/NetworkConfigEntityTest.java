package org.jetlinks.community.network.manager.entity;

import org.jetlinks.community.network.NetworkType;
import org.jetlinks.community.network.manager.enums.NetworkConfigState;
import org.junit.jupiter.api.Test;

import java.util.HashMap;

import static org.junit.jupiter.api.Assertions.*;

class NetworkConfigEntityTest {

    @Test
    void lookupNetworkType() {
        NetworkConfigEntity networkConfigEntity = new NetworkConfigEntity();
        networkConfigEntity.setId("test");
        networkConfigEntity.setName("test");
        networkConfigEntity.setDescription("test");
        networkConfigEntity.setType("TCP_CLIENT");
        networkConfigEntity.setState(NetworkConfigState.enabled);
        networkConfigEntity.setConfiguration(new HashMap<>());

        assertNotNull(networkConfigEntity.getName());
        assertNotNull(networkConfigEntity.getDescription());
        assertNotNull(networkConfigEntity.getType());
        assertNotNull(networkConfigEntity.getState());
        assertNotNull(networkConfigEntity.getConfiguration());


        NetworkType networkType = networkConfigEntity.lookupNetworkType();
        assertNotNull(networkType);
        NetworkType type = networkConfigEntity.lookupNetworkType();
        assertNotNull(type);

    }
}
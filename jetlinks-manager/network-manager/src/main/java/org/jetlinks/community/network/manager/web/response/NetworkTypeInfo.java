package org.jetlinks.community.network.manager.web.response;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.jetlinks.community.network.NetworkType;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class NetworkTypeInfo {

    private String id;

    private String name;


    public static NetworkTypeInfo of(NetworkType type) {

        NetworkTypeInfo info = new NetworkTypeInfo();

        info.setId(type.getId());
        info.setName(type.getName());

        return info;

    }

}

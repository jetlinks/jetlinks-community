package org.jetlinks.community.network;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public abstract class AbstractClientNetworkConfig implements ClientNetworkConfig {

    protected String id;

    protected String remoteHost;

    protected int remotePort;

    protected boolean secure;

    protected String certId;

}

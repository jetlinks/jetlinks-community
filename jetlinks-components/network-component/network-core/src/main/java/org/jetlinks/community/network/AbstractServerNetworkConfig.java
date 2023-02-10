package org.jetlinks.community.network;

import lombok.Getter;
import lombok.Setter;
import org.hibernate.validator.constraints.Range;
import org.jetlinks.community.network.resource.NetworkTransport;

import javax.validation.constraints.NotBlank;

@Getter
@Setter
public abstract class AbstractServerNetworkConfig implements ServerNetworkConfig {

    @NotBlank
    protected String id;

    protected boolean publicSecure;

    protected String publicCertId;

    @NotBlank
    protected String publicHost;

    @Range(min = 1, max = 65535)
    protected int publicPort;

    @NotBlank
    protected String host = "0.0.0.0";

    @Range(min = 1, max = 65535)
    protected int port;

    protected boolean secure;

    protected String certId;

    @Override
    public abstract NetworkTransport getTransport();


}

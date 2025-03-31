package org.jetlinks.community.device.web.request;

import lombok.Getter;
import lombok.Setter;
import org.jetlinks.community.protocol.ProtocolSupportEntity;

@Getter
@Setter
public class ProtocolEncodeRequest {

    ProtocolSupportEntity entity;

    ProtocolEncodePayload request;


}

package org.jetlinks.community.network.channel;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.net.URI;

@Getter
@Setter
@AllArgsConstructor(staticName = "of")
@NoArgsConstructor
public class Address {

    //正常
    public static final byte HEALTH_OK = 1;
    //无法访问
    public static final byte HEALTH_BAD = 0;
    //已禁用
    public static final byte HEALTH_DISABLED = -1;

    @Schema(description = "地址")
    private String address;

    @Schema(description = "健康状态,1:正常,0:无法访问,-1:已禁用")
    private byte health;

    public boolean isOk() {
        return health == HEALTH_OK;
    }

    public boolean isBad() {
        return health == HEALTH_BAD;
    }

    public boolean isDisabled() {
        return health == HEALTH_DISABLED;
    }

    public URI addressToUri() {
        return URI.create(address);
    }
}

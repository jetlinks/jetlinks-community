package org.jetlinks.community.network;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor(staticName = "of")
@NoArgsConstructor
public class NetworkUsage {
    @Schema(description = "使用者类型,如设备接入网关.")
    private String userType;

    @Schema(description = "使用者ID")
    private String userId;

    @Schema(description = "网络组件ID")
    private String networkId;
}

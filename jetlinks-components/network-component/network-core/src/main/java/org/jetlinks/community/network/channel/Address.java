/*
 * Copyright 2025 JetLinks https://www.jetlinks.cn
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
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

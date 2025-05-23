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
package org.jetlinks.community.device.web.response;

import lombok.*;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class DeviceDeployResult {

    private int total;

    private boolean success;

    private String message;

    private String sourceId;

    //导致错误的源头
    private Object source;

    //导致错误的操作
    private String operation;

    @Generated
    public static DeviceDeployResult success(int total) {
        return new DeviceDeployResult(total, true, null,null, null, null);
    }

    @Generated
    public static DeviceDeployResult error(String message) {
        return new DeviceDeployResult(0, false, message, null,null, null);
    }

    public static DeviceDeployResult error(String message, String sourceId) {
        return new DeviceDeployResult(0, false, message, sourceId,null, null);
    }
}

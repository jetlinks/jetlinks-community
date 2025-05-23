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
package org.jetlinks.community.device.entity;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public class DeviceLatestData extends HashMap<String,Object> {

    public DeviceLatestData(int initialCapacity, float loadFactor) {
        super(initialCapacity, loadFactor);
    }

    public DeviceLatestData(int initialCapacity) {
        super(initialCapacity);
    }

    public DeviceLatestData() {
    }

    public DeviceLatestData(Map<? extends String, ?> m) {
        super(m);
    }

    @Schema(description = "设备ID")
    public String getDeviceId(){
        return (String)Optional.ofNullable(get("deviceId")).orElseGet(()->get("id"));
    }


    @Schema(description = "设备名称")
    public String getDeviceName(){
        return (String)get("deviceName");
    }
}

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
package org.jetlinks.community.device.enums;

import lombok.AllArgsConstructor;
import lombok.Generated;
import lombok.Getter;
import org.hswebframework.web.dict.Dict;
import org.hswebframework.web.dict.EnumDict;
import org.jetlinks.core.metadata.Feature;

import java.util.Arrays;

@Dict("device-feature")
@Getter
@AllArgsConstructor
@Generated
public enum DeviceFeature implements EnumDict<String>, Feature {
    selfManageState("子设备自己管理状态")


    ;
    private final String text;

    @Override
    @Generated
    public String getValue() {
        return name();
    }

    @Override
    public String getId() {
        return getValue();
    }

    @Override
    public String getName() {
        return text;
    }

    public static DeviceFeature get(String id) {
        return Arrays.stream(values())
            .filter(deviceFeature -> deviceFeature.getId().equals(id))
            .findAny()
            .orElse(null);
    }
}

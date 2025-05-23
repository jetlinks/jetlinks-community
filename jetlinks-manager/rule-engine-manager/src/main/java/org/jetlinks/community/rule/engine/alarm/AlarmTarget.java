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
package org.jetlinks.community.rule.engine.alarm;

import reactor.core.publisher.Flux;

/**
 * 告警目标
 *
 * @author bestfeng
 */
public interface AlarmTarget {


    String getType();

    String getName();

    default Integer getOrder(){
        return Integer.MAX_VALUE;
    };

    Flux<AlarmTargetInfo> convert(AlarmData data);

    default boolean isSupported(String trigger) {
        return true;
    };

    static AlarmTarget of(String type) {
        return AlarmTargetSupplier
            .get()
            .getByType(type)
            .orElseThrow(() -> new UnsupportedOperationException("error.unsupported_alarm_target"));
    }
}

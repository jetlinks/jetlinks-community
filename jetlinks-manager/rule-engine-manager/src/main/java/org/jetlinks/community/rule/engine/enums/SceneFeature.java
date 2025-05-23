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
package org.jetlinks.community.rule.engine.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.jetlinks.core.metadata.Feature;

/**
 * 场景特性.
 *
 * @author zhangji 2024/6/11
 */
@AllArgsConstructor
@Getter
public enum SceneFeature implements Feature {

    none("不存在任何特性"),
    alarmTrigger("存在告警触发动作"),
    alarmReliever("存在告警解除动作");

    private final String name;

    @Override
    public String getId() {
        return name();
    }

}

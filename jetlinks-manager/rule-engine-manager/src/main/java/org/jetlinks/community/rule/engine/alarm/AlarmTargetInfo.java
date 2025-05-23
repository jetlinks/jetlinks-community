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

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * @author bestfeng
 */
@AllArgsConstructor(staticName = "of")
@NoArgsConstructor
@Getter
@Setter
public class AlarmTargetInfo {

    private String targetId;

    private String targetName;

    private String targetType;

    private String sourceType;

    private String sourceId;

    private String sourceName;

    private String creatorId;

    public static AlarmTargetInfo of(String targetId, String targetName, String targetType) {
        return AlarmTargetInfo.of(targetId, targetName, targetType, null, null, null, null);
    }

    public static AlarmTargetInfo of(String targetId, String targetName, String targetType, String creatorId) {
        return AlarmTargetInfo.of(targetId, targetName, targetType, null, null, null, creatorId);
    }

    public AlarmTargetInfo withTarget(String type, String id, String name) {
        this.targetType = type;
        this.targetId = id;
        this.targetName = name;
        return this;
    }

    public AlarmTargetInfo withSource(String type, String id, String name) {
        this.sourceType = type;
        this.sourceId = id;
        this.sourceName = name;
        return this;
    }
}

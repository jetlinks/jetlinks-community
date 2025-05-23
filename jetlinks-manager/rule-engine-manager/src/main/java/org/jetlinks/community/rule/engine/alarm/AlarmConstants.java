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

public interface AlarmConstants {

    interface ConfigKey {
        String alarmConfigId = "alarmConfigId";
        String alarming = "alarming";
        String firstAlarm = "firstAlarm";
        String alarmName = "alarmName";
        String level = "level";
        String ownerId = "ownerId";
        String description = "description";
        String state = "state";
        String alarmTime = "alarmTime";
        String lastAlarmTime = "lastAlarmTime";

        String targetType = "targetType";
        String targetId = "targetId";
        String targetName = "targetName";

        String sourceType = "sourceType";
        String sourceId = "sourceId";
        String sourceName = "sourceName";

        //告警条件
        String alarmFilterTermSpecs = "_filterTermSpecs";

        String alarmConfigSource = "alarmCenter";
    }
}

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
package org.jetlinks.community.rule.engine.cmd;

import org.jetlinks.core.command.AbstractCommandSupport;
import org.jetlinks.core.command.CommandHandler;
import org.jetlinks.community.command.rule.RelievedAlarmCommand;
import org.jetlinks.community.command.rule.TriggerAlarmCommand;
import org.jetlinks.community.rule.engine.alarm.AlarmHandler;

public class AlarmCommandSupport extends AbstractCommandSupport {

    public AlarmCommandSupport(AlarmHandler alarmHandler) {
        //触发告警
        registerHandler(
            TriggerAlarmCommand.class,
            CommandHandler.of(
                TriggerAlarmCommand::metadata,
                (cmd, ignore) -> alarmHandler
                    .triggerAlarm(cmd.getAlarmInfo()),
                TriggerAlarmCommand::new
            )
        );

        // 解除告警
        registerHandler(
            RelievedAlarmCommand.class,
            CommandHandler.of(
                RelievedAlarmCommand::metadata,
                (cmd, ignore) -> alarmHandler
                    .relieveAlarm(cmd.getRelieveInfo()),
                RelievedAlarmCommand::new
            )
        );
    }
}

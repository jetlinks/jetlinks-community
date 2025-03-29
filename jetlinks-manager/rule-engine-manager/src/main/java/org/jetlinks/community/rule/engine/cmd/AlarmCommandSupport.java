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

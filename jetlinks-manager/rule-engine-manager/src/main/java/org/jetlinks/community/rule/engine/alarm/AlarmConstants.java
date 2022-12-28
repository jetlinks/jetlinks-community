package org.jetlinks.community.rule.engine.alarm;

public interface AlarmConstants {

    interface ConfigKey {
        String alarmConfigId = "alarmConfigId";
        String alarming = "alarming";
        String firstAlarm = "firstAlarm";
        String alarmName = "alarmName";
        String level = "level";
        String ownerId = "ownerId";
        String state = "state";
        String alarmTime = "alarmTime";
        String lastAlarmTime = "lastAlarmTime";

        String targetType = "targetType";
        String targetId = "targetId";
        String targetName = "targetName";

        String sourceType = "sourceType";
        String sourceId = "sourceId";
        String sourceName = "sourceName";
    }
}

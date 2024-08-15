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

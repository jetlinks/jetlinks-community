package org.jetlinks.community.rule.engine.alarm;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * @author bestfeng
 */
@Getter
@Setter
@ConfigurationProperties(prefix = "jetlinks.alarm")
public class AlarmProperties {

    private AlarmHandleHistory handleHistory = new AlarmHandleHistory();


    @Getter
    @Setter
    public static class AlarmHandleHistory {

        //创建默认告警记录
        private boolean createWhenAlarm;
    }
}

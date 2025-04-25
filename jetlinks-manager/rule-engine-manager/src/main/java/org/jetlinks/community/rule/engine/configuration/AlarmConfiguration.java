package org.jetlinks.community.rule.engine.configuration;

import org.jetlinks.community.rule.engine.alarm.AlarmProperties;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

/**
 * @author bestfeng
 */
@AutoConfiguration
@EnableConfigurationProperties({AlarmProperties.class})
public class AlarmConfiguration {


}

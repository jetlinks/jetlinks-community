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
package org.jetlinks.community.device.service.data;

import lombok.Getter;
import lombok.Setter;
import org.jetlinks.core.config.ConfigKey;
import org.jetlinks.community.things.data.operations.DataSettings;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.HashMap;
import java.util.Map;

@ConfigurationProperties(prefix = "jetlinks.device.storage")
@Getter
@Setter
public class DeviceDataStorageProperties extends DataSettings {

    //默认数据存储策略,每个属性为一行数据
    private String defaultPolicy = "default-row";

    //是否保存最新数据到数据库
    private boolean enableLastDataInDb = false;

    private Metric metric = new Metric();

    @Getter
    @Setter
    public static class Metric {
        /**
         * 属性表前缀 {前缀}{产品ID}
         */
        private String propertyPrefix = "properties_";

        /**
         * 事件表前缀 {前缀}{产品ID}_{事件ID}
         * <p>
         * 如果设置了{@link Event#eventIsAllInOne()},
         * 则为  {前缀}{产品ID}_events
         */
        private String eventPrefix = "event_";

        /**
         * 日志表前缀 {前缀}{产品ID}
         */
        private String logPrefix = "device_log_";

        /**
         * 所有数据存储到日志表时的表名
         *
         * @see DeviceDataStorageProperties#getLog()
         * @see Log#isAllInOne()
         */
        private String logAllInOne = "device_all_log";

        /**
         * 存储策略自定义配置
         *
         * @see org.jetlinks.community.things.data.operations.MetricBuilder#option(ConfigKey)
         */
        private Map<String, Object> options = new HashMap<>();

    }

    public Log getLog() {
        return getLogFilter();
    }

}

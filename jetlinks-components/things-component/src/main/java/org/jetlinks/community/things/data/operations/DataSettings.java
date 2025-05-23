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
package org.jetlinks.community.things.data.operations;

import lombok.Getter;
import lombok.Setter;
import org.jetlinks.community.utils.MessageTypeMatcher;

@Getter
@Setter
public class DataSettings {

    private boolean strict = true;

    private Log logFilter = new Log();

    private Event event = new Event();

    private Property property = new Property();

    @Getter
    @Setter
    public static class Log extends MessageTypeMatcher {
        //使用同一个表来存储所有的日志数据
        private boolean allInOne = false;
    }

    @Getter
    @Setter
    public static class Property {
        //是否只保存属性上报消息
        private boolean onlySaveReport = false;
        //同时查询多个属性时使用聚合查询
        private boolean queryPropertiesAggregation = true;

    }

    @Getter
    @Setter
    public static class Event {
        public static final Event DEFAULT = new Event();

        //使用JSON字符来存储事件数据
        private boolean usingJsonString;
        //相同模版的事件数据使用同一个表来存储
        private boolean allInOne;

        //忽略未定义物模型的事件
        private boolean ignoreUndefined = true;

        public boolean eventIsAllInOne() {
            return usingJsonString && allInOne;
        }

        public boolean shouldIgnoreUndefined() {
            return ignoreUndefined || !eventIsAllInOne();
        }
    }
}
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
package org.jetlinks.community.notify.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.hswebframework.web.dict.I18nEnumDict;
import org.hswebframework.web.i18n.LocaleUtils;
import org.jetlinks.community.notify.subscription.SubscribeType;

/**
 * @author bestfeng
 */
@AllArgsConstructor
@Getter
public enum SubscriberTypeEnum implements SubscribeType, I18nEnumDict<String> {


    alarm("告警"),
    systemEvent("系统事件"),
    businessEvent("业务事件"),
    other("其它");

    private final String text;


    @Override
    public String getValue() {
        return name();
    }

    @Override
    public String getId() {
        return getValue();
    }

    @Override
    public String getName() {
        return getI18nMessage(LocaleUtils.current());
    }
}

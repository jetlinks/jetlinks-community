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
package org.jetlinks.community.notify.manager.subscriber.channel.notifiers;

import org.jetlinks.community.notify.DefaultNotifyType;
import org.jetlinks.community.notify.NotifierManager;
import org.jetlinks.community.notify.NotifyType;
import org.springframework.stereotype.Component;

@Component
public class SmsNotifyChannelProvider extends NotifierChannelProvider {
    public SmsNotifyChannelProvider(NotifierManager notifierManager) {
        super(notifierManager);
    }

    @Override
    protected NotifyType getNotifyType() {
        return DefaultNotifyType.sms;
    }

    @Override
    public int getOrder() {
        return 140;
    }
}

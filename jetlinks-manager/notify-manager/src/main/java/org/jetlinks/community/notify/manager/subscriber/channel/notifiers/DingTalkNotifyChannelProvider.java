package org.jetlinks.community.notify.manager.subscriber.channel.notifiers;

import org.jetlinks.community.notify.DefaultNotifyType;
import org.jetlinks.community.notify.NotifierManager;
import org.jetlinks.community.notify.NotifyType;
import org.springframework.stereotype.Component;

@Component
public class DingTalkNotifyChannelProvider extends NotifierChannelProvider {
    public DingTalkNotifyChannelProvider(NotifierManager notifierManager) {
        super(notifierManager);
    }

    @Override
    protected NotifyType getNotifyType() {
        return DefaultNotifyType.dingTalk;
    }

    @Override
    public int getOrder() {
        return 100;
    }
}

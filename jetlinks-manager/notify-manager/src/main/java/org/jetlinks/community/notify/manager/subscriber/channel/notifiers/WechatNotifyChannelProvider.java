package org.jetlinks.community.notify.manager.subscriber.channel.notifiers;

import org.jetlinks.community.notify.DefaultNotifyType;
import org.jetlinks.community.notify.NotifierManager;
import org.jetlinks.community.notify.NotifyType;
import org.springframework.stereotype.Component;

@Component
public class WechatNotifyChannelProvider extends NotifierChannelProvider {
    public WechatNotifyChannelProvider(NotifierManager notifierManager) {
        super(notifierManager);
    }

    @Override
    protected NotifyType getNotifyType() {
        return DefaultNotifyType.weixin;
    }

    @Override
    public int getOrder() {
        return 110;
    }
}

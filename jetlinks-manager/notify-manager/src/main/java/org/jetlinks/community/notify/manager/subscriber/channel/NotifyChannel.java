package org.jetlinks.community.notify.manager.subscriber.channel;

import org.jetlinks.community.notify.manager.entity.Notification;
import reactor.core.Disposable;
import reactor.core.publisher.Mono;

/**
 * 订阅通知通道,用于发送通知信息
 *
 * @author zhouhao
 * @since 2.0
 */
public interface NotifyChannel extends Disposable {

    Mono<Void> sendNotify(Notification notification);

}

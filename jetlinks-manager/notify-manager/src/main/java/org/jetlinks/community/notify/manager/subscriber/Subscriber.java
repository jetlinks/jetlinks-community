package org.jetlinks.community.notify.manager.subscriber;

import reactor.core.publisher.Flux;

import java.util.Locale;

public interface Subscriber {

    /**
     * 指定本地化语言发起订阅
     *
     * @param locale Locale
     * @return 通知事件流
     */
    Flux<Notify> subscribe(Locale locale);

    /**
     * 使用默认语言进行订阅
     *
     * @return 通知事件流
     */
    default Flux<Notify> subscribe() {
        return subscribe(Locale.getDefault());
    }

}

package org.jetlinks.community.notify.manager.subscriber;

import reactor.core.publisher.Flux;

import java.util.Locale;

public interface Subscriber {

    Flux<Notify> subscribe(Locale locale);

    default Flux<Notify> subscribe() {
        return subscribe(Locale.getDefault());
    }

}

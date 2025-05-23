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
package org.jetlinks.community.notify;

import org.jetlinks.core.event.EventBus;
import org.jetlinks.community.notify.event.NotifierEvent;
import org.jetlinks.community.notify.template.Template;
import reactor.core.publisher.Mono;

public class NotifierEventDispatcher<T extends Template> extends NotifierProxy<T> {

    private final EventBus eventBus;

    public NotifierEventDispatcher(EventBus eventBus, Notifier<T> target) {
        super(target);
        this.eventBus = eventBus;
    }

    @Override
    protected Mono<Void> onEvent(NotifierEvent event) {
        // /notify/{notifierId}/success

        return eventBus
            .publish(String.join("/", "/notify", event.getNotifierId(), event.isSuccess() ? "success" : "error"), event.toSerializable())
            .then();
    }


}

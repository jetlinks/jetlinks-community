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

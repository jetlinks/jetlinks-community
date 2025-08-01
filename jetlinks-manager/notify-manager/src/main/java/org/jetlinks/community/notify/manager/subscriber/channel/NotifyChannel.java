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

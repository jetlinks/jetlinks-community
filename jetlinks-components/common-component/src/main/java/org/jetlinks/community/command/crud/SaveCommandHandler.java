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
package org.jetlinks.community.command.crud;

import org.hswebframework.web.authorization.annotation.SaveAction;
import org.hswebframework.web.bean.FastBeanCopier;
import org.hswebframework.web.crud.service.ReactiveCrudService;
import org.jetlinks.core.annotation.command.CommandHandler;
import org.jetlinks.sdk.server.commons.cmd.AddCommand;
import org.jetlinks.sdk.server.commons.cmd.SaveCommand;
import org.jetlinks.sdk.server.commons.cmd.UpdateCommand;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;

public interface SaveCommandHandler<T, PK> {

    ReactiveCrudService<T, PK> getService();

    default T convertData(Object data) {
        return FastBeanCopier.copy(data, getService().getRepository().newInstanceNow());
    }

    @CommandHandler
    @SaveAction
    default Flux<T> save(SaveCommand<T> command) {
        List<T> data = command.dataList(this::convertData);
        return getService()
            .save(data)
            .thenMany(Flux.fromIterable(data));
    }

    @CommandHandler
    @SaveAction
    default Flux<T> add(AddCommand<T> command) {
        List<T> data = command.dataList(this::convertData);
        return getService()
            .save(data)
            .thenMany(Flux.fromIterable(data));
    }

    @CommandHandler
    @SaveAction
    default Mono<Integer> update(UpdateCommand<T> command) {
        return command
            .applyUpdate(getService().createUpdate(),
                         this::convertData)
            .execute();
    }
}

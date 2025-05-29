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

import com.google.common.collect.Collections2;
import org.hswebframework.web.api.crud.entity.PagerResult;
import org.hswebframework.web.authorization.annotation.QueryAction;
import org.hswebframework.web.crud.service.ReactiveCrudService;
import org.jetlinks.core.annotation.command.CommandHandler;
import org.jetlinks.sdk.server.commons.cmd.CountCommand;
import org.jetlinks.sdk.server.commons.cmd.QueryByIdCommand;
import org.jetlinks.sdk.server.commons.cmd.QueryListCommand;
import org.jetlinks.sdk.server.commons.cmd.QueryPagerCommand;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface QueryCommandHandler<T, PK> {

    ReactiveCrudService<T, PK> getService();

    @CommandHandler
    @QueryAction
    default Flux<T> queryById(QueryByIdCommand<T> command) {
        return getService()
            .findById(
                Collections2.transform(command.getIdList(),
                                       v -> (PK) v)
            );
    }

    @CommandHandler
    @QueryAction
    default Flux<T> queryList(QueryListCommand<T> command) {
        return getService().query(command.asQueryParam());
    }

    @CommandHandler
    @QueryAction
    default Mono<PagerResult<T>> queryPager(QueryPagerCommand<T> command) {
        return getService().queryPager(command.asQueryParam());
    }

    @CommandHandler
    @QueryAction
    default Mono<Integer> count(CountCommand command) {
        return getService().count(command.asQueryParam());
    }
}

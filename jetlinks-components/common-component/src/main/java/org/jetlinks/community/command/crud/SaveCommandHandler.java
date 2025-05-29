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

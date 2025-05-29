package org.jetlinks.community.command.crud;

import com.google.common.collect.Collections2;
import org.hswebframework.web.authorization.annotation.SaveAction;
import org.hswebframework.web.crud.service.ReactiveCrudService;
import org.jetlinks.core.annotation.command.CommandHandler;
import org.jetlinks.sdk.server.commons.cmd.DeleteByIdCommand;
import org.jetlinks.sdk.server.commons.cmd.DeleteCommand;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface DeleteCommandHandler<T, PK> {

    ReactiveCrudService<T, PK> getService();

    @CommandHandler
    @SaveAction
    default Mono<Integer> deleteById(DeleteByIdCommand<Integer> command) {
        return getService()
            .deleteById(Flux.fromIterable(
                Collections2
                    .transform(command.getIdList(),
                               v -> (PK) v)));
    }

    @CommandHandler
    @SaveAction
    default Mono<Integer> delete(DeleteCommand command) {
        return command
            .applyDelete(getService().createDelete())
            .execute();
    }
}

package org.jetlinks.community.command.crud;

import org.hswebframework.web.crud.service.ReactiveCrudService;

public interface CrudCommandHandler<T, PK>
    extends QueryCommandHandler<T, PK>, SaveCommandHandler<T, PK>, DeleteCommandHandler<T, PK> {

    @Override
    ReactiveCrudService<T, PK> getService();
}

package org.jetlinks.community.things.data.operations;

import org.hswebframework.web.api.crud.entity.PagerResult;
import org.hswebframework.web.api.crud.entity.QueryParamEntity;
import org.jetlinks.community.things.data.ThingProperties;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import javax.annotation.Nonnull;

public interface ColumnModeQueryOperations extends QueryOperations {

    @Nonnull
    Flux<ThingProperties> queryAllProperties(@Nonnull QueryParamEntity query);

    @Nonnull
    Mono<PagerResult<ThingProperties>> queryAllPropertiesPage(@Nonnull QueryParamEntity query);

}

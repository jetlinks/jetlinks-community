package org.jetlinks.community.notify.manager.service;

import org.hswebframework.web.api.crud.entity.PagerResult;
import org.hswebframework.web.api.crud.entity.QueryParamEntity;
import reactor.core.publisher.Mono;

public interface NotifyHistoryRepository {


    Mono<PagerResult<NotifyHistory>> queryPager(QueryParamEntity param);


}

package org.jetlinks.community.rule.engine.service;

import org.hswebframework.ezorm.core.param.QueryParam;
import org.hswebframework.web.api.crud.entity.PagerResult;
import org.jetlinks.community.rule.engine.entity.AlarmHistoryInfo;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * @author bestfeng
 */
public interface AlarmHistoryService {


    Mono<PagerResult<AlarmHistoryInfo>> queryPager(QueryParam queryParam);

    Mono<Void> save(AlarmHistoryInfo historyInfo);

    Mono<Void> save(Flux<AlarmHistoryInfo> historyInfo);

    Mono<Void> save(Mono<AlarmHistoryInfo> historyInfo);
}

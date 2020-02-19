package org.jetlinks.community.logging.service;


import org.hswebframework.ezorm.core.param.QueryParam;
import org.hswebframework.web.api.crud.entity.PagerResult;
import org.jetlinks.community.elastic.search.service.ElasticSearchService;
import org.jetlinks.community.logging.event.handler.LoggerIndexProvider;
import org.jetlinks.community.logging.system.SerializableSystemLog;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

/**
 * @version 1.0
 **/
@Service
public class SystemLoggerService {

    @Autowired
    private ElasticSearchService searchService;

    public Mono<PagerResult<SerializableSystemLog>> getSystemLogger(QueryParam queryParam) {
        return searchService.queryPager(LoggerIndexProvider.SYSTEM, queryParam, SerializableSystemLog.class);
    }
}

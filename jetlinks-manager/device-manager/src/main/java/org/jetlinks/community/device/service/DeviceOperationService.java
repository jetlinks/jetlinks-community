package org.jetlinks.community.device.service;

import lombok.extern.slf4j.Slf4j;
import org.hswebframework.ezorm.core.param.QueryParam;
import org.hswebframework.web.api.crud.entity.PagerResult;
import org.jetlinks.community.device.events.handler.DeviceIndexProvider;
import org.jetlinks.community.device.logger.DeviceOperationLog;
import org.jetlinks.community.elastic.search.service.ElasticSearchService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

/**
 * @author bsetfeng
 * @since 1.0
 **/
@Service
@Slf4j
public class DeviceOperationService {

    @Autowired
    private ElasticSearchService elasticSearchService;


    public Mono<PagerResult<DeviceOperationLog>> queryPager(QueryParam queryParam) {
        return elasticSearchService.queryPager(
                DeviceIndexProvider.DEVICE_OPERATION,
                queryParam,
                DeviceOperationLog.class
        );
    }
}

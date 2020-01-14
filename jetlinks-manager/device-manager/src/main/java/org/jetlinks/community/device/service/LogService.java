package org.jetlinks.community.device.service;

import lombok.extern.slf4j.Slf4j;
import org.hswebframework.ezorm.core.param.QueryParam;
import org.hswebframework.web.api.crud.entity.PagerResult;
import org.jetlinks.community.device.events.handler.DeviceEventIndex;
import org.jetlinks.community.elastic.search.service.ElasticSearchService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.util.Map;

/**
 * @author bsetfeng
 * @since 1.0
 **/
@Service
@Slf4j
public class LogService {

    @Autowired
    private ElasticSearchService queryService;

    public Mono<PagerResult<Map>> queryPagerByDeviceEvent(QueryParam queryParam, String productId, String eventId) {
        return queryService.queryPager(DeviceEventIndex.getDeviceEventIndex(productId, eventId), queryParam, Map.class);
    }
}

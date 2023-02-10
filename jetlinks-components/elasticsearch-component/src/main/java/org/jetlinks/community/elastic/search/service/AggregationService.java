package org.jetlinks.community.elastic.search.service;

import org.jetlinks.community.timeseries.query.AggregationQueryParam;
import reactor.core.publisher.Flux;

import java.util.Map;

/**
 * @author bsetfeng
 * @since 1.0
 **/
public interface AggregationService {

    Flux<Map<String, Object>> aggregation(String[] index, AggregationQueryParam queryParam);

    /**
     * @param index      索引
     * @param queryParam 聚合查询参数
     * @return 查询结果
     * @see AggregationService#aggregation(String[], AggregationQueryParam)
     */
    default Flux<Map<String, Object>> aggregation(String index, AggregationQueryParam queryParam) {
        return aggregation(new String[]{index}, queryParam);
    }
}

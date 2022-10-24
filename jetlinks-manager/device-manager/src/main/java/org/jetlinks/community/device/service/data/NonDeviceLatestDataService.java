package org.jetlinks.community.device.service.data;

import org.hswebframework.web.api.crud.entity.QueryParamEntity;
import org.jetlinks.core.message.DeviceMessage;
import org.jetlinks.core.metadata.DeviceMetadata;
import org.jetlinks.community.device.entity.DeviceLatestData;
import org.jetlinks.community.timeseries.query.AggregationColumn;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Map;

public class NonDeviceLatestDataService implements DeviceLatestDataService {
    @Override
    public Mono<Void> upgradeMetadata(String productId, DeviceMetadata metadata) {
        return Mono.empty();
    }

    @Override
    public Mono<Void> reloadMetadata(String productId, DeviceMetadata metadata) {
        return Mono.empty();
    }

    @Override
    public void save(DeviceMessage message) {

    }

    @Override
    public Flux<DeviceLatestData> query(String productId, QueryParamEntity param) {
        return Flux.empty();
    }

    @Override
    public Mono<DeviceLatestData> queryDeviceData(String productId, String deviceId) {
        return Mono.empty();
    }

    @Override
    public Mono<Integer> count(String productId, QueryParamEntity param) {
        return Mono.empty();
    }

    @Override
    public Mono<Map<String, Object>> aggregation(String productId, List<AggregationColumn> columns, QueryParamEntity paramEntity) {
        return Mono.empty();
    }

    @Override
    public Flux<Map<String, Object>> aggregation(Flux<QueryProductLatestDataRequest> param, boolean merge) {
        return Flux.empty();
    }
}

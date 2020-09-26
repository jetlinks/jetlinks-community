package org.jetlinks.community.device.service.data;

import org.hswebframework.web.api.crud.entity.PagerResult;
import org.hswebframework.web.api.crud.entity.QueryParamEntity;
import org.jetlinks.community.timeseries.TimeSeriesData;
import org.jetlinks.community.timeseries.TimeSeriesManager;
import org.jetlinks.core.device.DeviceRegistry;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.function.Function;

public abstract class TimeSeriesDeviceDataStoragePolicy extends AbstractDeviceDataStoragePolicy {


    protected TimeSeriesManager timeSeriesManager;

    public TimeSeriesDeviceDataStoragePolicy(DeviceRegistry registry,
                                             TimeSeriesManager timeSeriesManager,
                                             DeviceDataStorageProperties properties) {
        super(registry, properties);
        this.timeSeriesManager = timeSeriesManager;
    }

    protected Mono<Void> doSaveData(String metric, TimeSeriesData data) {
        return timeSeriesManager
            .getService(metric)
            .commit(data);
    }

    protected Mono<Void> doSaveData(String metric, Flux<TimeSeriesData> data) {
        return timeSeriesManager
            .getService(metric)
            .save(data);
    }

    protected <T> Flux<T> doQuery(String metric,
                                  QueryParamEntity paramEntity,
                                  Function<TimeSeriesData, T> mapper) {
        return timeSeriesManager
            .getService(metric)
            .query(paramEntity)
            .map(mapper);
    }


    protected <T> Mono<PagerResult<T>> doQueryPager(String metric,
                                                    QueryParamEntity paramEntity,
                                                    Function<TimeSeriesData, T> mapper) {
        return timeSeriesManager
            .getService(metric)
            .queryPager(paramEntity, mapper);
    }
}

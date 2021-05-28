package org.jetlinks.community.device.service.data;

import org.hswebframework.web.api.crud.entity.PagerResult;
import org.hswebframework.web.api.crud.entity.QueryParamEntity;
import org.jetlinks.community.timeseries.TimeSeriesData;
import org.jetlinks.community.timeseries.TimeSeriesManager;
import org.jetlinks.core.device.DeviceRegistry;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.function.Function;

/**
 * 抽象时序数据存储策略
 * <p>
 * 提供时序数据通用的查询存储逻辑
 * </p>
 *
 * @author zhouhao
 */
public abstract class TimeSeriesDeviceDataStoragePolicy extends AbstractDeviceDataStoragePolicy {


    protected TimeSeriesManager timeSeriesManager;

    public TimeSeriesDeviceDataStoragePolicy(DeviceRegistry registry,
                                             TimeSeriesManager timeSeriesManager,
                                             DeviceDataStorageProperties properties) {
        super(registry, properties);
        this.timeSeriesManager = timeSeriesManager;
    }

    @Override
    protected Mono<Void> doSaveData(String metric, TimeSeriesData data) {
        return timeSeriesManager
            .getService(metric)
            .commit(data);
    }

    @Override
    protected Mono<Void> doSaveData(String metric, Flux<TimeSeriesData> data) {
        return timeSeriesManager
            .getService(metric)
            .save(data);
    }

    @Override
    protected <T> Flux<T> doQuery(String metric,
                                  QueryParamEntity paramEntity,
                                  Function<TimeSeriesData, T> mapper) {
        return timeSeriesManager
            .getService(metric)
            .query(paramEntity)
            .map(mapper);
    }


    @Override
    protected <T> Mono<PagerResult<T>> doQueryPager(String metric,
                                                    QueryParamEntity paramEntity,
                                                    Function<TimeSeriesData, T> mapper) {
        return timeSeriesManager
            .getService(metric)
            .queryPager(paramEntity, mapper);
    }
}

package org.jetlinks.community.device.service;

import lombok.extern.slf4j.Slf4j;
import org.hswebframework.ezorm.core.param.QueryParam;
import org.hswebframework.web.api.crud.entity.PagerResult;
import org.hswebframework.web.bean.FastBeanCopier;
import org.hswebframework.web.crud.service.GenericReactiveCrudService;
import org.jetlinks.core.device.DeviceRegistry;
import org.jetlinks.core.device.ProductInfo;
import org.jetlinks.community.device.entity.DeviceProductEntity;
import org.jetlinks.community.device.entity.excel.ESDevicePropertiesEntity;
import org.jetlinks.community.device.enums.DeviceProductState;
import org.jetlinks.community.device.events.DeviceProductDeployEvent;
import org.jetlinks.community.device.events.handler.DeviceEventIndex;
import org.jetlinks.community.device.events.handler.DeviceIndexProvider;
import org.jetlinks.community.device.logger.DeviceOperationLog;
import org.jetlinks.community.elastic.search.service.ElasticSearchService;
import org.reactivestreams.Publisher;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.util.Map;

@Service
@Slf4j
public class LocalDeviceProductService extends GenericReactiveCrudService<DeviceProductEntity, String> {

    @Autowired
    private DeviceRegistry registry;

    @Autowired
    private ApplicationEventPublisher eventPublisher;

    @Autowired
    private ElasticSearchService elasticSearchService;

    public Mono<Integer> deploy(String id) {
        return findById(Mono.just(id))
            .flatMap(product -> registry.registry(new ProductInfo(id, product.getMessageProtocol(), product.getMetadata()))
                .flatMap(deviceProductOperator -> deviceProductOperator.setConfigs(product.getConfiguration()))
                .flatMap(re -> createUpdate()
                    .set(DeviceProductEntity::getState, DeviceProductState.registered.getValue())
                    .where(DeviceProductEntity::getId, id)
                    .execute())
                .doOnNext(i -> {
                    log.debug("设备型号：{}发布成功", product.getName());
                    eventPublisher.publishEvent(FastBeanCopier.copy(product, new DeviceProductDeployEvent()));
                })
            );
    }


    public Mono<Integer> cancelDeploy(String id) {
        return createUpdate()
            .set(DeviceProductEntity::getState, DeviceProductState.unregistered.getValue())
            .where(DeviceProductEntity::getId, id)
            .execute();

    }

    @Override
    public Mono<Integer> deleteById(Publisher<String> idPublisher) {
        // TODO: 2019/12/5 校验是否可以删除
        return super.deleteById(idPublisher);
    }

    public Mono<PagerResult<DeviceOperationLog>> getDeviceLog(QueryParam param) {
        return elasticSearchService
            .queryPager(DeviceIndexProvider.DEVICE_OPERATION, param, DeviceOperationLog.class);
    }


    public Mono<PagerResult<Map>> getDeviceEvents(String productId, String eventId, QueryParam param) {
        return elasticSearchService.queryPager(DeviceEventIndex.getDeviceEventIndex(productId, eventId), param, Map.class);
    }

    /**
     * 查询型号下面所有设备属性
     * @param productId 型号id
     * @param queryParam 查询条件
     * @return
     */
    public Mono<PagerResult<ESDevicePropertiesEntity>> getProperties(String productId, QueryParam queryParam) {
        return elasticSearchService
            .queryPager(DeviceEventIndex.getDevicePropertiesIndex(productId), queryParam, ESDevicePropertiesEntity.class);
    }
}

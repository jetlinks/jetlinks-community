/*
 * Copyright 2025 JetLinks https://www.jetlinks.cn
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.jetlinks.community.device.service;

import com.github.benmanes.caffeine.cache.Caffeine;
import lombok.extern.slf4j.Slf4j;
import org.hswebframework.ezorm.rdb.mapping.ReactiveRepository;
import org.hswebframework.ezorm.rdb.mapping.defaults.SaveResult;
import org.hswebframework.web.api.crud.entity.PagerResult;
import org.hswebframework.web.api.crud.entity.QueryParamEntity;
import org.hswebframework.web.bean.FastBeanCopier;
import org.hswebframework.web.crud.events.EntityEventHelper;
import org.hswebframework.web.crud.events.EntityPrepareSaveEvent;
import org.hswebframework.web.crud.service.GenericReactiveCrudService;
import org.hswebframework.web.exception.BusinessException;
import org.jetlinks.core.device.DeviceProductOperator;
import org.jetlinks.core.device.DeviceRegistry;
import org.jetlinks.core.metadata.DataType;
import org.jetlinks.core.metadata.DeviceMetadata;
import org.jetlinks.core.metadata.types.ObjectType;
import org.jetlinks.community.device.entity.*;
import org.jetlinks.community.device.enums.DeviceProductState;
import org.jetlinks.community.device.events.DeviceProductDeployEvent;
import org.jetlinks.community.device.timeseries.DeviceTimeSeriesMetric;
import org.jetlinks.community.timeseries.TimeSeriesManager;
import org.jetlinks.supports.official.JetLinksDeviceMetadataCodec;
import org.reactivestreams.Publisher;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Assert;
import org.springframework.util.CollectionUtils;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.util.function.Tuple2;
import reactor.util.function.Tuples;

import java.time.Duration;
import java.util.*;

@Service
@Slf4j
public class LocalDeviceProductService extends GenericReactiveCrudService<DeviceProductEntity, String> {

    private final DeviceRegistry registry;

    private final ApplicationEventPublisher eventPublisher;

    private final ReactiveRepository<DeviceInstanceEntity, String> instanceRepository;

    private final TimeSeriesManager timeSeriesManager;

    private final DeviceConfigMetadataManager metadataManager;

    private final Map<String, Mono<DeviceProductEntity>> productLoadCache = Caffeine
        .newBuilder()
        .expireAfterWrite(Duration.ofSeconds(2))
        .softValues()
        .<String, Mono<DeviceProductEntity>>build()
        .asMap();

    @SuppressWarnings("all")
    public LocalDeviceProductService(DeviceRegistry registry,
                                     ApplicationEventPublisher eventPublisher,
                                     ReactiveRepository<DeviceInstanceEntity, String> instanceRepository,
                                     TimeSeriesManager timeSeriesManager,
                                     DeviceConfigMetadataManager metadataManager) {
        this.registry = registry;
        this.eventPublisher = eventPublisher;
        this.instanceRepository = instanceRepository;
        this.timeSeriesManager = timeSeriesManager;
        this.metadataManager = metadataManager;
    }

    /**
     * 并行支持的根据ID查询产品,用于在可能并行查询同一个产品id的场景,减少查询压力.
     *
     * @param id ID
     * @return 产品实体
     */
    public Mono<DeviceProductEntity> concurrentFindById(String id) {
        return productLoadCache.computeIfAbsent(id, _id -> this
            .findById(id)
            .cache(val -> Duration.ofSeconds(2), err -> Duration.ZERO, () -> Duration.ofSeconds(1)));
    }

    @EventListener
    public void handlePrepareEvent(EntityPrepareSaveEvent<DeviceProductEntity> event){
        event.async(
            Flux
                .fromIterable(event.getEntity())
                .doOnNext(product -> product.setState(null))
        );
    }

    @Override
    public Mono<SaveResult> save(Publisher<DeviceProductEntity> entityPublisher) {
        return Flux.from(entityPublisher)
                   .flatMap(product -> validateMetadata(product.getId(), product.getMetadata()).thenReturn(product))
                   .as(super::save);
    }

    @Override
    public Mono<Integer> updateById(String id, Mono<DeviceProductEntity> entityPublisher) {
        return super.updateById(id, entityPublisher
            .doOnNext(product -> product.setState(null))
            .flatMap(product -> validateMetadata(product.getId(), product.getMetadata()).thenReturn(product))
        );
    }

    private Mono<Void> validateMetadata(String productId, String metadata) {
        // FIXME: 2021/8/23 更好的校验方式
        JetLinksDeviceMetadataCodec.getInstance().doDecode(metadata);
        return Mono.empty();
    }

    public Mono<PagerResult<ProductDetail>> queryProductDetail(QueryParamEntity entity) {
        return this
            .queryPager(entity)
            .filter(e -> !CollectionUtils.isEmpty(e.getData()))
            .flatMap(result -> this
                .convertDetail(result.getData())
                .collectList()
                .map(detailList -> PagerResult.of(result.getTotal(), detailList, entity)))
            .defaultIfEmpty(PagerResult.empty());
    }

    public Flux<ProductDetail> queryProductDetailList(QueryParamEntity entity) {
        return this
            .query(entity)
            .collectList()
            .flatMapMany(this::convertDetail);
    }

    @Transactional
    public Mono<Integer> deploy(String id) {
        return findById(Mono.just(id))
            .doOnNext(this::validateDeviceProduct)
            // 检查协议相关配置的必填项
            .flatMap(product -> validateConfiguration(product).thenReturn(product))
            .flatMap(product -> registry
                .register(product.toProductInfo())
                .onErrorMap(e -> new BusinessException("error.unable_to_load_protocol_by_access_id", 404, product.getMessageProtocol()))
                .then(
                    createUpdate()
                        .set(DeviceProductEntity::getState, DeviceProductState.registered.getValue())
                        .where(DeviceProductEntity::getId, id)
                        .execute()
                        .as(EntityEventHelper::setDoNotFireEvent)
                )
                .flatMap(i -> FastBeanCopier
                    .copy(product, new DeviceProductDeployEvent())
                    .publish(eventPublisher)
                    .thenReturn(i))
            );
    }

    private void validateDeviceProduct(DeviceProductEntity product) {
        // 设备接入ID不能为空
//        Assert.hasText(product.getAccessId(), "error.access_id_can_not_be_empty");
        // 发布前，必须填写消息协议
        Assert.hasText(product.getMessageProtocol(), "error.please_select_the_access_mode_first");
    }

    private Mono<Void> validateConfiguration(DeviceProductEntity product) {
        return metadataManager
            .getProductConfigMetadata(product.getId())
            .flatMap(configMetadata -> DeviceConfigMetadataManager.validate(configMetadata, product.getConfiguration()))
            .filter(validateResult -> !validateResult.isSuccess())
            .flatMap(validateResult -> {
                if (log.isDebugEnabled()) {
                    log.debug(validateResult.getErrorMsg());
                }
                return Mono.error(() -> new BusinessException("error.product_configuration_required_must_not_be_null"));
            })
            .then();
    }


    public Mono<Integer> cancelDeploy(String id) {
        return createUpdate()
            .set(DeviceProductEntity::getState, DeviceProductState.unregistered.getValue())
            .where(DeviceProductEntity::getId, id)
            .execute()
            .flatMap(integer ->
                         registry
                             .unregisterProduct(id)
                             .thenReturn(integer))
            ;

    }

    @Override
    public Mono<Integer> deleteById(Publisher<String> idPublisher) {
        return Flux.from(idPublisher)
                   .collectList()
                   .flatMap(idList ->
                                instanceRepository.createQuery()
                                                  .where()
                                                  .in(DeviceInstanceEntity::getProductId, idList)
                                                  .count()
                                                  .flatMap(i -> {
                                                      if (i > 0) {
                                                          return Mono.error(new IllegalArgumentException("error.cannot_deleted_because_device_is_associated_with_it"));
                                                      } else {
                                                          return super.deleteById(Flux.fromIterable(idList));
                                                      }
                                                  }));
    }

    @Deprecated
    public Mono<PagerResult<Map<String, Object>>> queryDeviceEvent(String productId,
                                                                   String eventId,
                                                                   QueryParamEntity entity,
                                                                   boolean format) {
        return registry
            .getProduct(productId)
            .flatMap(operator -> Mono.just(operator.getId()).zipWith(operator.getMetadata()))
            .flatMap(tp -> timeSeriesManager
                .getService(DeviceTimeSeriesMetric.deviceEventMetric(tp.getT1(), eventId))
                .queryPager(entity, data -> {
                    if (!format) {
                        return data.getData();
                    }
                    Map<String, Object> formatData = new HashMap<>(data.getData());
                    tp.getT2()
                      .getEvent(eventId)
                      .ifPresent(eventMetadata -> {
                          DataType type = eventMetadata.getType();
                          if (type instanceof ObjectType) {
                              @SuppressWarnings("all")
                              Map<String, Object> val = (Map<String, Object>) type.format(formatData);
                              val.forEach((k, v) -> formatData.put(k + "_format", v));
                          } else {
                              formatData.put("value_format", type.format(data.get("value")));
                          }
                      });
                    return formatData;
                })
            ).defaultIfEmpty(PagerResult.empty());
    }

    /**
     * 将产品的物模型合并到产品下设备的独立物模型中
     *
     * @param productId 产品ID
     * @return void
     */
    public Mono<Void> mergeMetadataToDevice(String productId) {
        return this
            .findById(productId)
            .flatMap(product -> JetLinksDeviceMetadataCodec.getInstance().decode(product.getMetadata()))
            .flatMap(metadata -> instanceRepository
                .createQuery()
                //查询出产品下配置了独立物模型的设备
                .where(DeviceInstanceEntity::getProductId, productId)
                .notNull(DeviceInstanceEntity::getDeriveMetadata)
                .notEmpty(DeviceInstanceEntity::getDeriveMetadata)
                .fetch()
                //合并物模型
                .flatMap(instance -> instance.mergeMetadata(metadata).thenReturn(instance))
                //更新物模型
                .flatMap(instance -> instanceRepository
                    .createUpdate()
                    .set(instance::getDeriveMetadata)
                    .where(instance::getId)
                    .execute()
                    .then(registry.getDevice(instance.getId()))
                    .flatMap(device -> device.updateMetadata(instance.getDeriveMetadata()))
                )
                .then());
    }

    @Deprecated
    public Mono<PagerResult<DevicePropertiesEntity>> queryDeviceProperties(String productId, QueryParamEntity entity) {
        return timeSeriesManager
            .getService(DeviceTimeSeriesMetric.devicePropertyMetric(productId))
            .queryPager(entity, data -> data.as(DevicePropertiesEntity.class))
            .defaultIfEmpty(PagerResult.empty());
    }

    @Deprecated
    public Mono<PagerResult<DeviceOperationLogEntity>> queryDeviceLog(String productId, QueryParamEntity entity) {
        return timeSeriesManager
            .getService(DeviceTimeSeriesMetric.deviceLogMetric(productId))
            .queryPager(entity, data -> data.as(DeviceOperationLogEntity.class))
            .defaultIfEmpty(PagerResult.empty());
    }

    //转换为详情信息
    private Flux<ProductDetail> convertDetail(List<DeviceProductEntity> list) {
        return Flux
            .fromIterable(list)
            .index()
            .flatMap(
                indexTp2 -> Flux
                    .zip(
                        //产品所需配置信息
                        metadataManager
                            .getProductConfigMetadata(indexTp2.getT2().getId())
                            .collectList()
                            .onErrorReturn(Collections.emptyList()),
                        //产品特性
                        metadataManager
                            .getProductFeatures(indexTp2.getT2().getId())
                            .collectList()
                            .onErrorReturn(Collections.emptyList()),
                        //设备数量
                        instanceRepository
                            .createQuery()
                            .where()
                            .and(DeviceInstanceEntity::getProductId, indexTp2.getT2().getId())
                            .count()
                    )
                    .map(tp3 -> Tuples
                        .of(indexTp2.getT1(),
                            ProductDetail
                                .of()
                                .with(indexTp2.getT2())
                                .withConfigMetadatas(tp3.getT1())
                                .withFeatures(tp3.getT2())
                                .withDeviceCount(tp3.getT3()))
                    )
                ,
                16)
            //重新排序
            .sort(Comparator.comparingLong(Tuple2::getT1))
            .map(Tuple2::getT2);
    }


    public Mono<DeviceMetadata> getMetadata(String productId) {
        return registry
            .getProduct(productId)
            .flatMap(DeviceProductOperator::getMetadata)
            .switchIfEmpty(
                this
                    .findById(productId)
                    .flatMap(product -> JetLinksDeviceMetadataCodec
                        .getInstance()
                        .decode(product.getMetadata()))
            );
    }
}

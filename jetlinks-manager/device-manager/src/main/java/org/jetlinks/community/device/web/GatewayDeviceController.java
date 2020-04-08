package org.jetlinks.community.device.web;

import com.alibaba.fastjson.JSON;
import org.apache.commons.collections4.CollectionUtils;
import org.hswebframework.web.api.crud.entity.PagerResult;
import org.hswebframework.web.api.crud.entity.QueryParamEntity;
import org.hswebframework.web.authorization.annotation.QueryAction;
import org.hswebframework.web.authorization.annotation.Resource;
import org.hswebframework.web.authorization.annotation.SaveAction;
import org.jetlinks.core.device.DeviceConfigKey;
import org.jetlinks.core.device.DeviceRegistry;
import org.jetlinks.community.device.entity.DeviceInstanceEntity;
import org.jetlinks.community.device.entity.DeviceProductEntity;
import org.jetlinks.community.device.enums.DeviceType;
import org.jetlinks.community.device.service.LocalDeviceInstanceService;
import org.jetlinks.community.device.service.LocalDeviceProductService;
import org.jetlinks.community.device.web.response.GatewayDeviceInfo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 网关设备接口
 *
 * @author zhouhao
 * @since 1.0
 */
@RestController
@RequestMapping("/device/gateway")
@Resource(id = "device-gateway", name = "网关设备管理")
public class GatewayDeviceController {

    @Autowired
    private LocalDeviceInstanceService instanceService;

    @Autowired
    private LocalDeviceProductService productService;

    @Autowired
    private DeviceRegistry registry;

    @SuppressWarnings("all")
    private Mono<List<String>> getGatewayProductList() {
        return productService
            .createQuery()
            .select(DeviceProductEntity::getId)
            .where(DeviceProductEntity::getDeviceType, DeviceType.gateway)
            .fetch()
            .map(DeviceProductEntity::getId)
            .collectList()
            .filter(CollectionUtils::isNotEmpty);
    }

    @GetMapping("/_query")
    @QueryAction
    public Mono<PagerResult<GatewayDeviceInfo>> queryGatewayDevice(QueryParamEntity param) {
        return getGatewayProductList()
            .flatMap(productIdList ->
                param.toNestQuery(q -> q.in(DeviceInstanceEntity::getProductId, productIdList))
                    .execute(instanceService::queryPager)
                    .filter(r -> r.getTotal() > 0)
                    .flatMap(result -> {
                        Map<String, DeviceInstanceEntity> mapping =
                            result.getData()
                                .stream()
                                .collect(Collectors.toMap(DeviceInstanceEntity::getId, Function.identity()));

                        //查询所有子设备
                        return instanceService.createQuery()
                            .where()
                            .in(DeviceInstanceEntity::getParentId, mapping.keySet())
                            .fetch()
                            .groupBy(DeviceInstanceEntity::getParentId)
                            .flatMap(group -> {
                                String parentId = group.key();
                                return group
                                    .collectList()
                                    .map(children -> GatewayDeviceInfo.of(mapping.get(parentId), children));
                            })
                            .collectMap(GatewayDeviceInfo::getId)//收集所有有子设备的网关设备信息
                            .defaultIfEmpty(Collections.emptyMap())
                            .flatMapMany(map -> Flux.fromIterable(mapping.values())
                                .flatMap(ins -> Mono.justOrEmpty(map.get(ins.getId()))
                                    //处理没有子设备的网关信息
                                    .switchIfEmpty(Mono.fromSupplier(() -> GatewayDeviceInfo.of(ins, Collections.emptyList())))))
                            .collectList()
                            .map(list -> PagerResult.of(result.getTotal(), list, param));
                    }))
            .defaultIfEmpty(PagerResult.empty());
    }

    @GetMapping("/{id}")
    @QueryAction
    public Mono<GatewayDeviceInfo> getGatewayInfo(@PathVariable String id) {
        return Mono.zip(
            instanceService.findById(id),
            instanceService.createQuery()
                .where()
                .is(DeviceInstanceEntity::getParentId, id)
                .fetch()
                .collectList()
                .defaultIfEmpty(Collections.emptyList()),
            GatewayDeviceInfo::of);
    }


    @PostMapping("/{gatewayId}/bind/{deviceId}")
    @SaveAction
    public Mono<GatewayDeviceInfo> bindDevice(@PathVariable String gatewayId,
                                              @PathVariable String deviceId) {
        return instanceService
            .createUpdate()
            .set(DeviceInstanceEntity::getParentId, gatewayId)
            .where(DeviceInstanceEntity::getId, deviceId)
            .execute()
            .then(registry
                .getDevice(deviceId)
                .flatMap(operator -> operator.setConfig(DeviceConfigKey.parentGatewayId, gatewayId)))
            .then(getGatewayInfo(gatewayId));
    }

    @PostMapping("/{gatewayId}/bind")
    @SaveAction
    public Mono<GatewayDeviceInfo> bindDevice(@PathVariable String gatewayId,
                                              @RequestBody Flux<String> deviceId) {

        return deviceId
            .flatMapIterable(str -> {
                if (str.startsWith("[")) {
                    return JSON.parseArray(str, String.class);
                }
                return Collections.singletonList(str);
            })
            .collectList()
            .filter(CollectionUtils::isNotEmpty)
            .flatMap(deviceIdList -> instanceService
                .createUpdate()
                .set(DeviceInstanceEntity::getParentId, gatewayId)
                .where()
                .in(DeviceInstanceEntity::getId, deviceIdList)
                .execute()
                .then(Flux
                    .fromIterable(deviceIdList)
                    .flatMap(id -> registry
                        .getDevice(id)
                        .flatMap(operator -> operator.setConfig(DeviceConfigKey.parentGatewayId, gatewayId))).then()
                )
            ).then(getGatewayInfo(gatewayId));

    }

    @PostMapping("/{gatewayId}/unbind/{deviceId}")
    @SaveAction
    public Mono<GatewayDeviceInfo> unBindDevice(@PathVariable String gatewayId,
                                                @PathVariable String deviceId) {
        return instanceService
            .createUpdate()
            .setNull(DeviceInstanceEntity::getParentId)
            .where(DeviceInstanceEntity::getId, deviceId)
            .and(DeviceInstanceEntity::getParentId, gatewayId)
            .execute()
            .filter(i -> i > 0)
            .flatMap(i -> registry
                .getDevice(deviceId)
                .flatMap(operator -> operator.removeConfig(DeviceConfigKey.parentGatewayId.getKey())))
            .then(getGatewayInfo(gatewayId));
    }
}

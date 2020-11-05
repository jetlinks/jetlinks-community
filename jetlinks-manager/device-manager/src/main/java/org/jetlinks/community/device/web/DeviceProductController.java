package org.jetlinks.community.device.web;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hswebframework.web.authorization.annotation.QueryAction;
import org.hswebframework.web.authorization.annotation.Resource;
import org.hswebframework.web.authorization.annotation.SaveAction;
import org.hswebframework.web.crud.web.reactive.ReactiveServiceCrudController;
import org.jetlinks.community.device.entity.DeviceProductEntity;
import org.jetlinks.community.device.service.DeviceConfigMetadataManager;
import org.jetlinks.community.device.service.LocalDeviceProductService;
import org.jetlinks.community.device.service.data.DeviceDataService;
import org.jetlinks.community.device.service.data.DeviceDataStoragePolicy;
import org.jetlinks.community.device.web.request.AggRequest;
import org.jetlinks.community.timeseries.query.AggregationData;
import org.jetlinks.core.metadata.ConfigMetadata;
import org.jetlinks.core.metadata.DeviceMetadataCodec;
import org.jetlinks.supports.official.JetLinksDeviceMetadataCodec;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping({"/device-product","/device/product"})
@Resource(id = "device-product", name = "设备产品")
@Tag(name = "设备产品接口")
public class DeviceProductController implements ReactiveServiceCrudController<DeviceProductEntity, String> {

    private final LocalDeviceProductService productService;

    private final List<DeviceDataStoragePolicy> policies;

    private final DeviceDataService deviceDataService;

    private final DeviceConfigMetadataManager configMetadataManager;

    private final ObjectProvider<DeviceMetadataCodec> metadataCodecs;

    private final DeviceMetadataCodec defaultCodec = new JetLinksDeviceMetadataCodec();

    public DeviceProductController(LocalDeviceProductService productService,
                                   List<DeviceDataStoragePolicy> policies,
                                   DeviceDataService deviceDataService,
                                   DeviceConfigMetadataManager configMetadataManager,
                                   ObjectProvider<DeviceMetadataCodec> metadataCodecs) {
        this.productService = productService;
        this.policies = policies;
        this.deviceDataService = deviceDataService;
        this.configMetadataManager = configMetadataManager;
        this.metadataCodecs = metadataCodecs;
    }

    @Override
    public LocalDeviceProductService getService() {
        return productService;
    }

    @GetMapping("/{id:.+}/config-metadata")
    @QueryAction
    @Operation(summary = "获取产品需要的配置定义信息")
    public Flux<ConfigMetadata> getDeviceConfigMetadata(@PathVariable
                                                        @Parameter(description = "产品ID") String id) {
        return configMetadataManager.getProductConfigMetadata(id);
    }

    @GetMapping("/metadata/codecs")
    @QueryAction
    @Operation(summary = "获取支持的物模型格式")
    public Flux<DeviceMetadataCodec> getMetadataCodec() {
        return Flux.fromIterable(metadataCodecs);
    }

    @PostMapping("/metadata/convert-to/{id}")
    @QueryAction
    @Operation(summary = "转换平台的物模型为指定的物模型格式")
    public Mono<String> convertMetadataTo(@RequestBody Mono<String> metadata,
                                          @PathVariable String id) {

        return metadata
            .flatMap(str -> Flux
                .fromIterable(metadataCodecs)
                .filter(codec -> codec.getId().equals(id))
                .next()
                .flatMap(codec -> defaultCodec
                    .decode(str)
                    .flatMap(codec::encode)));
    }

    @PostMapping("/metadata/convert-from/{id}")
    @QueryAction
    @Operation(summary = "转换指定的物模型为平台的物模型格式")
    public Mono<String> convertMetadataFrom(@RequestBody Mono<String> metadata,
                                            @PathVariable String id) {

        return metadata
            .flatMap(str -> Flux
                .fromIterable(metadataCodecs)
                .filter(codec -> codec.getId().equals(id))
                .next()
                .flatMap(codec -> codec
                    .decode(str)
                    .flatMap(defaultCodec::encode)));
    }

    @PostMapping("/{productId:.+}/deploy")
    @SaveAction
    @Operation(summary = "激活产品")
    public Mono<Integer> deviceDeploy(@PathVariable @Parameter(description = "产品ID") String productId) {
        return productService.deploy(productId);
    }

    @PostMapping("/{productId:.+}/undeploy")
    @SaveAction
    @Operation(summary = "注销产品")
    public Mono<Integer> cancelDeploy(@PathVariable @Parameter(description = "产品ID") String productId) {
        return productService.cancelDeploy(productId);
    }

    @GetMapping("/storage/policies")
    @Operation(summary = "获取支持的数据存储策略")
    public Flux<DeviceDataStorePolicyInfo> storePolicy() {
        return Flux.fromIterable(policies)
            .flatMap(DeviceDataStorePolicyInfo::of);
    }

    @PostMapping("/{productId:.+}/agg/_query")
    @QueryAction
    @Operation(summary = "聚合查询产品下设备属性")
    public Flux<Map<String, Object>> aggDeviceProperty(@PathVariable
                                                       @Parameter(description = "产品ID") String productId,
                                                       @RequestBody Mono<AggRequest> param) {

        return param
            .flatMapMany(request -> deviceDataService
                .aggregationPropertiesByProduct(productId,
                    request.getQuery(),
                    request.getColumns().toArray(new DeviceDataService.DevicePropertyAggregation[0]))
            )
            .map(AggregationData::values);
    }

    @Getter
    @Setter
    @AllArgsConstructor
    @NoArgsConstructor
    public static class DeviceDataStorePolicyInfo {
        private String id;

        private String name;

        private String description;

        private ConfigMetadata configMetadata;

        public static Mono<DeviceDataStorePolicyInfo> of(DeviceDataStoragePolicy policy) {
            return policy.getConfigMetadata()
                .map(metadata -> new DeviceDataStorePolicyInfo(policy.getId(), policy.getName(), policy.getDescription(), metadata))
                .defaultIfEmpty(new DeviceDataStorePolicyInfo(policy.getId(), policy.getName(), policy.getDescription(), null));
        }
    }

}

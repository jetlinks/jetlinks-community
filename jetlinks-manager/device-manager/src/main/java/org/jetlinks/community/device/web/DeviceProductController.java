package org.jetlinks.community.device.web;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.hswebframework.reactor.excel.ReactorExcel;
import org.hswebframework.web.authorization.annotation.QueryAction;
import org.hswebframework.web.authorization.annotation.Resource;
import org.hswebframework.web.authorization.annotation.SaveAction;
import org.hswebframework.web.crud.web.reactive.ReactiveServiceCrudController;
import org.hswebframework.web.exception.ValidationException;
import org.hswebframework.web.i18n.LocaleUtils;
import org.jetlinks.community.device.entity.DeviceProductEntity;
import org.jetlinks.community.device.service.DeviceConfigMetadataManager;
import org.jetlinks.community.device.service.LocalDeviceProductService;
import org.jetlinks.community.device.service.data.DeviceDataService;
import org.jetlinks.community.device.web.excel.PropertyMetadataExcelInfo;
import org.jetlinks.community.device.web.excel.PropertyMetadataWrapper;
import org.jetlinks.community.device.web.request.AggRequest;
import org.jetlinks.community.io.excel.ImportExportService;
import org.jetlinks.community.io.utils.FileUtils;
import org.jetlinks.community.things.data.ThingsDataRepositoryStrategy;
import org.jetlinks.community.timeseries.query.AggregationData;
import org.jetlinks.community.web.response.ValidationResult;
import org.jetlinks.core.metadata.*;
import org.jetlinks.supports.official.JetLinksDeviceMetadataCodec;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.core.io.buffer.DataBufferFactory;
import org.springframework.core.io.buffer.DefaultDataBufferFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

import static org.hswebframework.reactor.excel.ReactorExcel.read;

@RestController
@RequestMapping({"/device-product","/device/product"})
@Resource(id = "device-product", name = "设备产品")
@Tag(name = "设备产品接口")
@Slf4j
public class DeviceProductController implements ReactiveServiceCrudController<DeviceProductEntity, String> {

    private final LocalDeviceProductService productService;

    private final List<ThingsDataRepositoryStrategy> policies;
    private final DeviceDataService deviceDataService;

    private final DeviceConfigMetadataManager configMetadataManager;

    private final ObjectProvider<DeviceMetadataCodec> metadataCodecs;

    private final DeviceMetadataCodec defaultCodec = new JetLinksDeviceMetadataCodec();


    private final DataBufferFactory bufferFactory = new DefaultDataBufferFactory();

    private final ImportExportService importExportService;

    public DeviceProductController(LocalDeviceProductService productService,
                                   List<ThingsDataRepositoryStrategy> policies,
                                   DeviceDataService deviceDataService,
                                   DeviceConfigMetadataManager configMetadataManager,
                                   ObjectProvider<DeviceMetadataCodec> metadataCodecs,
                                   ImportExportService importExportService) {
        this.productService = productService;
        this.policies = policies;
        this.deviceDataService = deviceDataService;
        this.configMetadataManager = configMetadataManager;
        this.metadataCodecs = metadataCodecs;
        this.importExportService = importExportService;
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

    @GetMapping("/{id:.+}/{accessId:.+}/config-metadata")
    @QueryAction
    @Operation(summary = "根据指定的接入方式获取产品需要的配置定义信息")
    public Flux<ConfigMetadata> getProductConfigMetadataByAccessId(@PathVariable @Parameter(description = "产品ID") String id,
                                                                   @PathVariable
                                                                   @Parameter(description = "接入方式ID") String accessId) {
        return configMetadataManager.getProductConfigMetadataByAccessId(id, accessId);
    }


    @GetMapping("/{id:.+}/config-metadata/{metadataType}/{metadataId}/{typeId}")
    @QueryAction
    @Operation(summary = "获取产品物模型的拓展配置定义")
    public Flux<ConfigMetadata> getExpandsConfigMetadata(@PathVariable @Parameter(description = "产品ID") String id,
                                                         @PathVariable @Parameter(description = "物模型类型") DeviceMetadataType metadataType,
                                                         @PathVariable @Parameter(description = "物模型ID") String metadataId,
                                                         @PathVariable @Parameter(description = "类型ID") String typeId) {
        return configMetadataManager.getMetadataExpandsConfig(id, metadataType, metadataId, typeId, DeviceConfigScope.product);
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
            .map(DeviceDataStorePolicyInfo::of);
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

        public static DeviceDataStorePolicyInfo of(ThingsDataRepositoryStrategy strategy) {
            return new DeviceDataStorePolicyInfo(strategy.getId(), strategy.getName(), null, null);
        }
    }

    @GetMapping("/{id:.+}/exists")
    @QueryAction
    @Operation(summary = "验证产品ID是否存在")
    public Mono<Boolean> deviceIdValidate(@PathVariable @Parameter(description = "产品ID") String id) {
        return productService.findById(id)
                             .hasElement();
    }

    @GetMapping("/id/_validate")
    @QueryAction
    @Operation(summary = "验证产品ID是否合法")
    public Mono<ValidationResult> deviceIdValidate2(@RequestParam @Parameter(description = "产品ID") String id) {
        return LocaleUtils.currentReactive()
                          .flatMap(locale -> {
                              DeviceProductEntity entity = new DeviceProductEntity();
                              entity.setId(id);
                              entity.validateId();

                              return productService.findById(id)
                                                   .map(product -> ValidationResult.error(
                                                       LocaleUtils.resolveMessage("error.product_ID_already_exists", locale)))
                                                   .defaultIfEmpty(ValidationResult.success());
                          })
                          .onErrorResume(ValidationException.class, e -> Mono.just(e.getI18nCode())
                                                                             .map(ValidationResult::error));
    }


    //获取产品物模型属性导入模块
    @GetMapping("/{productId}/property-metadata/template.{format}")
    @QueryAction
    @Operation(summary = "下载产品物模型属性导入模块")
    public Mono<Void> downloadExportPropertyMetadataTemplate(@PathVariable @Parameter(description = "产品ID") String productId,
                                                             ServerHttpResponse response,
                                                             @PathVariable @Parameter(description = "文件格式,支持csv,xlsx") String format) throws IOException {
        response.getHeaders().set(HttpHeaders.CONTENT_DISPOSITION,
                                  "attachment; filename=".concat(URLEncoder.encode("物模型导入模块." + format, StandardCharsets.UTF_8
                                      .displayName())));

        return configMetadataManager
            .getMetadataExpandsConfig(productId, DeviceMetadataType.property, "*", "*", DeviceConfigScope.product)
            .collectList()
            .map(PropertyMetadataExcelInfo::getTemplateHeaderMapping)
            .flatMapMany(headers -> ReactorExcel
                .<PropertyMetadataExcelInfo>writer(format)
                .headers(headers)
                .converter(PropertyMetadataExcelInfo::toMap)
                .writeBuffer(PropertyMetadataExcelInfo.getTemplateContentMapping()))
            .doOnError(err -> log.error(err.getMessage(), err))
            .map(bufferFactory::wrap)
            .as(response::writeWith)
            ;
    }

    //解析文件为属性物模型
    @PostMapping(value = "/{productId}/property-metadata/import")
    @SaveAction
    @Operation(summary = "解析文件为属性物模型")
    public Mono<String> importPropertyMetadata(@PathVariable @Parameter(description = "产品ID") String productId,
                                               @RequestParam @Parameter(description = "文件地址,支持csv,xlsx文件格式") String fileUrl) {
        return configMetadataManager
            .getMetadataExpandsConfig(productId, DeviceMetadataType.property, "*", "*", DeviceConfigScope.product)
            .collectList()
            .map(PropertyMetadataWrapper::new)
            //解析数据并转为物模型
            .flatMap(wrapper -> importExportService
                .getInputStream(fileUrl)
                .flatMapMany(inputStream -> read(inputStream, FileUtils.getExtension(fileUrl), wrapper))
                .map(PropertyMetadataExcelInfo::toMetadata)
                .collectList())
            .filter(CollectionUtils::isNotEmpty)
            .map(list -> {
                SimpleDeviceMetadata metadata = new SimpleDeviceMetadata();
                list.forEach(metadata::addProperty);
                return JetLinksDeviceMetadataCodec.getInstance().doEncode(metadata);
            });
    }

}

package org.jetlinks.community.device.web;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.AllArgsConstructor;
import org.hswebframework.ezorm.rdb.mapping.ReactiveRepository;
import org.hswebframework.web.authorization.annotation.QueryAction;
import org.hswebframework.web.authorization.annotation.Resource;
import org.hswebframework.web.authorization.annotation.SaveAction;
import org.hswebframework.web.i18n.LocaleUtils;
import org.jetlinks.community.device.entity.TransparentMessageCodecEntity;
import org.jetlinks.community.device.message.transparent.TransparentMessageCodecProviders;
import org.jetlinks.community.device.web.request.TransparentMessageCodecRequest;
import org.jetlinks.community.device.web.request.TransparentMessageDecodeRequest;
import org.jetlinks.community.device.web.response.TransparentMessageDecodeResponse;
import org.jetlinks.core.device.DeviceOperator;
import org.jetlinks.core.device.DeviceProductOperator;
import org.jetlinks.core.device.DeviceRegistry;
import org.jetlinks.core.metadata.DeviceMetadata;
import org.jetlinks.core.utils.TypeScriptUtils;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/device/transparent-codec")
@Tag(name = "设备透传消息解析配置")
@AllArgsConstructor
@Resource(id = "transparent-codec", name = "设备透传消息解析配置")
public class TransparentMessageCodecController {

    private final ReactiveRepository<TransparentMessageCodecEntity, String> repository;

    private final DeviceRegistry registry;


    @PostMapping("/decode-test")
    @QueryAction
    @Operation(summary = "测试解码")
    public Mono<TransparentMessageDecodeResponse> getCodec(@RequestBody Mono<TransparentMessageDecodeRequest> requestMono) {
        return requestMono
            .flatMapMany(req -> TransparentMessageCodecProviders
                .getProviderNow(req.getProvider())
                .createCodec(req.getConfiguration())
                .flatMapMany(codec -> codec.decode(req.toMessage())))
            .collectList()
            .map(TransparentMessageDecodeResponse::of)
            .onErrorResume(err -> LocaleUtils.doWithReactive(
                err,
                Throwable::getLocalizedMessage,
                (e, msg) -> TransparentMessageDecodeResponse.error(msg)));
    }

    @GetMapping("/{productId}/{deviceId}.d.ts")
    @QueryAction
    @Operation(summary = "获取设备的TypeScript定义信息")
    public Mono<String> getTypescriptDeclares(@PathVariable String productId,
                                              @PathVariable String deviceId) {
        return registry
            .getDevice(deviceId)
            .flatMap(DeviceOperator::getMetadata)
            .flatMap(this::getTypescriptDeclares);
    }

    @GetMapping("/{productId}.d.ts")
    @QueryAction
    @Operation(summary = "获取产品的TypeScript定义信息")
    public Mono<String> getTypescriptDeclares(@PathVariable String productId) {
        return registry
            .getProduct(productId)
            .flatMap(DeviceProductOperator::getMetadata)
            .flatMap(this::getTypescriptDeclares);
    }


    @GetMapping("/{productId}/{deviceId}")
    @QueryAction
    @Operation(summary = "获取设备的解析规则")
    public Mono<TransparentMessageCodecEntity> getCodec(@PathVariable String productId,
                                                        @PathVariable String deviceId) {


        return repository
            .findById(TransparentMessageCodecEntity.createId(productId, deviceId))
            //设备没有则获取产品的
            .switchIfEmpty(Mono.defer(() -> {
                if (StringUtils.hasText(deviceId)) {
                    return repository.findById(TransparentMessageCodecEntity.createId(productId, null));
                }
                return Mono.empty();
            }));
    }

    @GetMapping("/{productId}")
    @QueryAction
    @Operation(summary = "获取产品的解析规则")
    public Mono<TransparentMessageCodecEntity> getCodec(@PathVariable String productId) {

        return getCodec(productId, null);
    }


    @PostMapping("/{productId}/{deviceId}")
    @SaveAction
    @Operation(summary = "保存设备解析规则")
    public Mono<Void> saveCodec(@PathVariable String productId,
                                @PathVariable String deviceId,
                                @RequestBody Mono<TransparentMessageCodecRequest> requestMono) {


        return requestMono
            .flatMap(request-> {
                TransparentMessageCodecEntity codec = new TransparentMessageCodecEntity();
                codec.setProductId(productId);
                codec.setDeviceId(deviceId);
                codec.setProvider(request.getProvider());
                codec.setConfiguration(request.getConfiguration());
                return repository.save(codec);
            })
            .then();
    }

    @PostMapping("/{productId}")
    @Operation(summary = "保存产品解析规则")
    public Mono<Void> saveCodec(@PathVariable String productId,
                                @RequestBody Mono<TransparentMessageCodecRequest> requestMono) {
        return saveCodec(productId, null, requestMono);
    }

    @DeleteMapping("/{productId}/{deviceId}")
    @SaveAction
    @Operation(summary = "重置设备的解析规则")
    public Mono<Void> removeCodec(@PathVariable String productId,
                                  @PathVariable String deviceId) {


        return repository
            .deleteById(TransparentMessageCodecEntity.createId(productId, deviceId))
            .then();
    }

    @DeleteMapping("/{productId}")
    @SaveAction
    @Operation(summary = "重置产品的解析规则")
    public Mono<Void> removeCodec(@PathVariable String productId) {
        return removeCodec(productId, null);
    }


    private Mono<String> getTypescriptDeclares(DeviceMetadata metadata) {
        StringBuilder builder = new StringBuilder();

        TypeScriptUtils.createMetadataDeclare(metadata, builder);
        TypeScriptUtils.loadDeclare("transparent-codec", builder);

        return Mono.just(builder.toString());
    }

}

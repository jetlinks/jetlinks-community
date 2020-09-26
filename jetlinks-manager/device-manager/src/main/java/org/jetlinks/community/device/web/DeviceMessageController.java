package org.jetlinks.community.device.web;

import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.hswebframework.web.authorization.annotation.Authorize;
import org.hswebframework.web.authorization.annotation.Resource;
import org.hswebframework.web.exception.BusinessException;
import org.hswebframework.web.id.IDGenerator;
import org.jetlinks.community.device.entity.DevicePropertiesEntity;
import org.jetlinks.community.utils.ErrorUtils;
import org.jetlinks.core.device.DeviceOperator;
import org.jetlinks.core.device.DeviceRegistry;
import org.jetlinks.core.enums.ErrorCode;
import org.jetlinks.core.exception.DeviceOperationException;
import org.jetlinks.core.message.DeviceMessageReply;
import org.jetlinks.core.message.FunctionInvokeMessageSender;
import org.jetlinks.core.message.ReadPropertyMessageSender;
import org.jetlinks.core.message.WritePropertyMessageSender;
import org.jetlinks.core.message.function.FunctionInvokeMessageReply;
import org.jetlinks.core.message.property.ReadPropertyMessageReply;
import org.jetlinks.core.message.property.WritePropertyMessageReply;
import org.jetlinks.core.metadata.PropertyMetadata;
import org.jetlinks.core.metadata.types.StringType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Map;
import java.util.function.Function;

@RestController
@RequestMapping("/device")
@Slf4j
@Authorize
@Resource(id = "device-instance", name = "设备实例")
@Tag(name = "设备指令API")
@Deprecated
public class DeviceMessageController {

    @Autowired
    private DeviceRegistry registry;

    //获取设备属性
    @GetMapping("/{deviceId}/property/{property:.+}")
    @SneakyThrows
    @Deprecated
    public Flux<?> getProperty(@PathVariable String deviceId, @PathVariable String property) {

        return registry
            .getDevice(deviceId)
            .switchIfEmpty(ErrorUtils.notFound("设备不存在"))
            .map(DeviceOperator::messageSender)//发送消息到设备
            .map(sender -> sender.readProperty(property).messageId(IDGenerator.SNOW_FLAKE_STRING.generate()))
            .flatMapMany(ReadPropertyMessageSender::send)
            .map(mapReply(ReadPropertyMessageReply::getProperties));

    }

    //获取标准设备属性
    @GetMapping("/standard/{deviceId}/property/{property:.+}")
    @SneakyThrows
    @Deprecated
    public Mono<DevicePropertiesEntity> getStandardProperty(@PathVariable String deviceId, @PathVariable String property) {
        return Mono.from(registry
            .getDevice(deviceId)
            .switchIfEmpty(ErrorUtils.notFound("设备不存在"))
            .flatMapMany(deviceOperator -> deviceOperator.messageSender()
                .readProperty(property).messageId(IDGenerator.SNOW_FLAKE_STRING.generate())
                .send()
                .map(mapReply(ReadPropertyMessageReply::getProperties))
                .flatMap(map -> {
                    Object value = map.get(property);
                    return deviceOperator.getMetadata()
                        .map(deviceMetadata -> deviceMetadata.getProperty(property)
                            .map(PropertyMetadata::getValueType)
                            .orElse(new StringType()))
                        .map(dataType -> DevicePropertiesEntity.builder()
                            .deviceId(deviceId)
                            .productId(property)
                            .build()
                            .withValue(dataType, value));
                })))
            ;

    }

    //设置设备属性
    @PostMapping("/setting/{deviceId}/property")
    @SneakyThrows
    @Deprecated
    public Flux<?> settingProperties(@PathVariable String deviceId, @RequestBody Map<String, Object> properties) {

        return registry
            .getDevice(deviceId)
            .switchIfEmpty(ErrorUtils.notFound("设备不存在"))
            .map(operator -> operator
                .messageSender()
                .writeProperty()
                .messageId(IDGenerator.SNOW_FLAKE_STRING.generate())
                .write(properties)
            )
            .flatMapMany(WritePropertyMessageSender::send)
            .map(mapReply(WritePropertyMessageReply::getProperties));
    }

    //设备功能调用
    @PostMapping("invoked/{deviceId}/function/{functionId}")
    @SneakyThrows
    @Deprecated
    public Flux<?> invokedFunction(@PathVariable String deviceId,
                                   @PathVariable String functionId,
                                   @RequestBody Map<String, Object> properties) {

        return registry
            .getDevice(deviceId)
            .switchIfEmpty(ErrorUtils.notFound("设备不存在"))
            .flatMap(operator -> operator
                .messageSender()
                .invokeFunction(functionId)
                .messageId(IDGenerator.SNOW_FLAKE_STRING.generate())
                .setParameter(properties)
                .validate()
            )
            .flatMapMany(FunctionInvokeMessageSender::send)
            .map(mapReply(FunctionInvokeMessageReply::getOutput));
    }

    //获取设备所有属性
    @PostMapping("/{deviceId}/properties")
    @SneakyThrows
    @Deprecated
    public Flux<?> getProperties(@PathVariable String deviceId,
                                 @RequestBody Mono<List<String>> properties) {

        return registry.getDevice(deviceId)
            .switchIfEmpty(ErrorUtils.notFound("设备不存在"))
            .map(DeviceOperator::messageSender)
            .flatMapMany((sender) ->
                properties.flatMapMany(list ->
                    sender.readProperty(list.toArray(new String[0]))
                        .messageId(IDGenerator.SNOW_FLAKE_STRING.generate())
                        .send()))
            .map(mapReply(ReadPropertyMessageReply::getProperties));
    }

    private static <R extends DeviceMessageReply, T> Function<R, T> mapReply(Function<R, T> function) {
        return reply -> {
            if (ErrorCode.REQUEST_HANDLING.name().equals(reply.getCode())) {
                throw new DeviceOperationException(ErrorCode.REQUEST_HANDLING, reply.getMessage());
            }
            if (!reply.isSuccess()) {
                throw new BusinessException(reply.getMessage(), reply.getCode());
            }
            T mapped = function.apply(reply);
            if (mapped == null) {
                throw new BusinessException(reply.getMessage(), reply.getCode());
            }
            return mapped;
        };
    }
}

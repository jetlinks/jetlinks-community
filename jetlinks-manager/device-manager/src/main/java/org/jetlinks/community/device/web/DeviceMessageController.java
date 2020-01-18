package org.jetlinks.community.device.web;

import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.hswebframework.web.exception.NotFoundException;
import org.hswebframework.web.id.IDGenerator;
import org.jetlinks.community.device.entity.excel.ESDevicePropertiesEntity;
import org.jetlinks.core.device.DeviceOperator;
import org.jetlinks.core.device.DeviceRegistry;
import org.jetlinks.core.message.FunctionInvokeMessageSender;
import org.jetlinks.core.message.ReadPropertyMessageSender;
import org.jetlinks.core.message.WritePropertyMessageSender;
import org.jetlinks.core.message.function.FunctionInvokeMessageReply;
import org.jetlinks.core.message.property.ReadPropertyMessageReply;
import org.jetlinks.core.message.property.WritePropertyMessageReply;
import org.jetlinks.community.gateway.MessageGateway;
import org.jetlinks.community.gateway.TopicMessage;
import org.jetlinks.core.metadata.PropertyMetadata;
import org.jetlinks.core.metadata.types.StringType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/device")
@Slf4j
public class DeviceMessageController {

    private final DeviceRegistry registry;

    public final MessageGateway messageGateway;

    public DeviceMessageController(DeviceRegistry registry, MessageGateway messageGateway) {
        this.registry = registry;
        this.messageGateway = messageGateway;
    }


    //获取设备实时事件
    @GetMapping(value = "/{deviceId}/event", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<Object> getEvent(@PathVariable String deviceId) {
        return messageGateway
            .subscribe("/device/".concat(deviceId).concat("/message/event/**"))
            .map(TopicMessage::getMessage)
            .map(msg->msg.getPayload().toString(StandardCharsets.UTF_8))
            ;
    }

    //获取设备属性
    @GetMapping("/{deviceId}/property/{property:.+}")
    @SneakyThrows
    public Flux<?> getProperties(@PathVariable String deviceId, @PathVariable String property) {

        return registry
            .getDevice(deviceId)
            .switchIfEmpty(Mono.error(()->new NotFoundException("设备不存在")))
            .map(DeviceOperator::messageSender)
            .map(sender -> sender.readProperty(property).messageId(IDGenerator.SNOW_FLAKE_STRING.generate()))
            .flatMapMany(ReadPropertyMessageSender::send)
            .map(ReadPropertyMessageReply::getProperties);

    }

    //设置设备属性
    @PostMapping("setting/{deviceId}/property")
    @SneakyThrows
    public Flux<?> settingProperties(@PathVariable String deviceId, @RequestBody Map<String, Object> properties) {

        return registry
            .getDevice(deviceId)
            .switchIfEmpty(Mono.error(()->new NotFoundException("设备不存在")))
            .map(DeviceOperator::messageSender)
            .map(sender -> sender.writeProperty().messageId(IDGenerator.SNOW_FLAKE_STRING.generate()).write(properties))
            .flatMapMany(WritePropertyMessageSender::send)
            .map(WritePropertyMessageReply::getProperties);

    }

    //获取标准设备属性
    @GetMapping("/standard/{deviceId}/property/{property:.+}")
    @SneakyThrows
    public Mono<ESDevicePropertiesEntity> getStandardProperty(@PathVariable String deviceId, @PathVariable String property) {
        return Mono.from(registry
            .getDevice(deviceId)
            .switchIfEmpty(Mono.error(()->new NotFoundException("设备不存在")))
            .flatMapMany(deviceOperator -> deviceOperator.messageSender()
                .readProperty(property).messageId(IDGenerator.SNOW_FLAKE_STRING.generate())
                .send()
                .map(ReadPropertyMessageReply::getProperties)
                .flatMap(map -> {
                    Object value = map.get(property);
                    return deviceOperator.getMetadata()
                        .map(deviceMetadata -> deviceMetadata.getProperty(property)
                            .map(PropertyMetadata::getValueType)
                            .orElse(new StringType()))
                        .map(dataType -> {
                            ESDevicePropertiesEntity entity = new ESDevicePropertiesEntity();
                            if (value != null) {
                                entity.setDeviceId(deviceId);
                                entity.setProperty(property);
                                entity.setValue(value.toString());
                                entity.setStringValue(value.toString());
                                entity.setFormatValue(dataType.format(value).toString());

                            }
                            return entity;
                        });
                })))
            ;

    }

    //设备功能调用
    @PostMapping("invoked/{deviceId}/functionId/{functionId}")
    @SneakyThrows
    public Flux<?> invokedFunction(@PathVariable String deviceId, @PathVariable String functionId, @RequestBody Map<String, Object> properties) {

        return registry
            .getDevice(deviceId)
            .switchIfEmpty(Mono.error(()->new NotFoundException("设备不存在")))
            .map(DeviceOperator::messageSender)
            .map(sender -> sender
                .invokeFunction(functionId)
                .messageId(IDGenerator.SNOW_FLAKE_STRING.generate())
                .setParameter(properties))
            .flatMapMany(FunctionInvokeMessageSender::send)
            .map(FunctionInvokeMessageReply::getOutput)
            ;

    }


    //获取设备所有属性
    @PostMapping("/{deviceId}/properties")
    @SneakyThrows
    public Flux<?> getProperties(@PathVariable String deviceId,
                                 @RequestBody Mono<List<String>> properties) {

        return registry.getDevice(deviceId)
            .switchIfEmpty(Mono.error(()->new NotFoundException("设备不存在")))
            .map(DeviceOperator::messageSender)
            .flatMapMany((sender) ->
                properties.flatMapMany(list ->
                    sender.readProperty(list.toArray(new String[0]))
                        .messageId(IDGenerator.SNOW_FLAKE_STRING.generate())
                        .send()))
            .map(ReadPropertyMessageReply::getProperties)
            ;

    }
}

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
package org.jetlinks.community.device.cmd;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.hswebframework.ezorm.core.CastUtil;
import org.hswebframework.web.api.crud.entity.PagerResult;
import org.hswebframework.web.api.crud.entity.QueryParamEntity;
import org.hswebframework.web.authorization.Authentication;
import org.hswebframework.web.authorization.Permission;
import org.hswebframework.web.authorization.annotation.*;
import org.hswebframework.web.authorization.exception.AccessDenyException;
import org.hswebframework.web.bean.FastBeanCopier;
import org.hswebframework.web.crud.query.QueryHelper;
import org.hswebframework.web.crud.service.ReactiveCrudService;
import org.hswebframework.web.exception.BusinessException;
import org.hswebframework.web.id.IDGenerator;
import org.jetlinks.community.command.crud.CrudCommandHandler;
import org.jetlinks.community.device.DeviceManagerConstants;
import org.jetlinks.core.command.CommandHandler;
import org.jetlinks.core.command.CommandUtils;
import org.jetlinks.core.device.DeviceRegistry;
import org.jetlinks.core.event.EventBus;
import org.jetlinks.core.event.Subscription;
import org.jetlinks.core.event.TopicPayload;
import org.jetlinks.core.message.DeviceMessage;
import org.jetlinks.core.message.DeviceMessageReply;
import org.jetlinks.core.message.RepayableDeviceMessage;
import org.jetlinks.core.message.ThingMessage;
import org.jetlinks.core.message.function.FunctionInvokeMessageReply;
import org.jetlinks.core.message.property.ReadPropertyMessageReply;
import org.jetlinks.core.message.property.WritePropertyMessageReply;
import org.jetlinks.core.metadata.*;
import org.jetlinks.core.metadata.unit.ValueUnit;
import org.jetlinks.community.annotation.command.CommandService;
import org.jetlinks.community.device.entity.DeviceInstanceEntity;
import org.jetlinks.community.device.service.LocalDeviceInstanceService;
import org.jetlinks.community.device.service.data.DeviceDataService;
import org.jetlinks.community.device.web.request.AggRequest;
import org.jetlinks.community.gateway.DeviceMessageUtils;
import org.jetlinks.community.timeseries.query.AggregationData;
import org.jetlinks.sdk.server.commons.cmd.*;
import org.jetlinks.sdk.server.device.DeviceDetail;
import org.jetlinks.sdk.server.device.DeviceInfo;
import org.jetlinks.sdk.server.device.DeviceProperty;
import org.jetlinks.sdk.server.device.cmd.*;
import org.jetlinks.sdk.server.ui.field.annotation.field.form.QueryComponent;
import org.jetlinks.sdk.server.ui.field.annotation.field.select.DevicePropertySelector;
import org.jetlinks.sdk.server.ui.field.annotation.field.select.DeviceSelector;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 设备命令支持.
 *
 * @author zhangji 2024/1/15
 */
@CommandService(id = "deviceService:device", name = "设备命令支持")
@Resource(id = "device-instance", name = "设备实例")
@Authorize(anonymous = true)
@Component
public class DeviceCommandSupport implements CrudCommandHandler<DeviceInstanceEntity, String> {

    private final LocalDeviceInstanceService deviceInstanceService;

    private final DeviceDataService deviceDataService;

    private final DeviceRegistry registry;

    private final EventBus eventBus;

    public DeviceCommandSupport(LocalDeviceInstanceService deviceInstanceService,
                                DeviceDataService deviceDataService,
                                DeviceRegistry registry, EventBus eventBus) {
        this.deviceInstanceService = deviceInstanceService;
        this.deviceDataService = deviceDataService;
        this.registry = registry;
        this.eventBus = eventBus;

    }


    @org.jetlinks.core.annotation.command.CommandHandler
    @QueryAction
    public Mono<DeviceMetadata> getMetadata(GetMetadataCommand command) {
        return deviceInstanceService.getMetadata(command.getId());
    }

    @QueryAction
    @org.jetlinks.core.annotation.command.CommandHandler(name = "查询设备详情列表", description = "可指定查询条件，排序规则等")
    @QueryComponent(fields = DeviceInstanceEntity.class)
    public Flux<DeviceInfo> queryDevice(QueryListCommand<DeviceInfo> cmd) {
        return getService()
            .query(cmd.asQueryParam())
            .map(entity -> FastBeanCopier.copy(entity, new DeviceInfo()));
    }

    @QueryAction
    @org.jetlinks.core.annotation.command.CommandHandler(name = "分页查询设备详情")
    public Mono<PagerResult<DeviceInfo>> queryDevicePager(QueryPagerCommand<DeviceInfo> cmd) {
        return QueryHelper
            .transformPageResult(
                deviceInstanceService.queryPager(cmd.asQueryParam()),
                list -> Flux
                    .fromIterable(list)
                    .map(entity -> FastBeanCopier.copy(entity, new DeviceInfo()))
                    .collectList()
            );
    }

    @QueryAction
    @org.jetlinks.core.annotation.command.CommandHandler(id = "QueryById", name = "查询设备详情")
    public Mono<DeviceDetail> queryDeviceDetail(@DeviceSelector @Schema(title = "ID") String id) {
        return deviceInstanceService
            .getDeviceDetail(id)
            .map(detail -> FastBeanCopier.copy(detail, new DeviceDetail()));
    }

    @QueryAction
    @org.jetlinks.core.annotation.command.CommandHandler
    public Flux<DeviceProperty> queryPropertiesLatest(QueryPropertyLatestCommand cmd) {
        QueryParamEntity queryParamEntity = QueryParamEntity
            .newQuery()
            .orderByDesc("timestamp")
            .getParam();
        return deviceDataService
            .queryEachOneProperties(cmd.getId(), queryParamEntity)
            .map(property -> FastBeanCopier.copy(property, new DeviceProperty()));
    }

    @QueryAction
    @org.jetlinks.core.annotation.command.CommandHandler
    public Flux<Map<String, Object>> queryPropertiesAgg(QueryPropertyAggCommand cmd) {
        return
            Mono
                .fromSupplier(() -> {
                    AggRequest aggRequest = new AggRequest();
                    aggRequest.setColumns(
                        cmd
                            .getColumns()
                            .stream()
                            .map(map -> FastBeanCopier.copy(map, new DeviceDataService.DevicePropertyAggregation()))
                            .collect(Collectors.toList()));
                    aggRequest.setQuery(FastBeanCopier.copy(cmd.getQuery(), new DeviceDataService.AggregationRequest()));
                    return aggRequest;
                })
                .doOnNext(AggRequest::validate)
                .flatMapMany(request -> deviceDataService
                    .aggregationPropertiesByDevice(cmd.getId(),
                                                   request.getQuery(),
                                                   request
                                                       .getColumns()
                                                       .toArray(new DeviceDataService.DevicePropertyAggregation[0]))
                )
                .map(AggregationData::values);
    }

    @QueryAction
    @org.jetlinks.core.annotation.command.CommandHandler(name = "查询设备的指定属性值列表", description = "多个属性逗号隔开", id = "QueryPropertyList")
    public Flux<DeviceProperty> queryPropertyList(QueryDevicePropertyListCommand cmd) {
        return
            deviceDataService
                .queryProperty(cmd.getDeviceId(), cmd.asQueryParam(), cmd.getProperty().split(","))
                .map(prop -> FastBeanCopier.copy(prop, new DeviceProperty()));
    }

    @QueryAction
    @org.jetlinks.core.annotation.command.CommandHandler(name = "分页查询设备的指定属性值", description = "多个属性逗号隔开", id = "QueryPropertyPage")
    public Mono<PagerResult<DeviceProperty>> queryPropertyPager(QueryDeviceQueryPropertyPageCommand cmd) {
        return QueryHelper
            .transformPageResult(
                deviceDataService.queryPropertyPage(cmd.getDeviceId(), cmd.asQueryParam(), cmd
                    .getProperty()
                    .split(",")
                ),
                list -> Flux
                    .fromIterable(list)
                    .map(prop -> FastBeanCopier.copy(prop, new DeviceProperty()))
                    .collectList()
            );
    }

    /**
     * 执行设备消息命令（设备功能调用、获取设备属性、设置设备属性）
     *
     * @param deviceMessage 消息
     * @return Flux<R>
     */
    public <R extends DeviceMessageReply> Flux<? extends ThingMessage> executeDeviceMessageCommand(RepayableDeviceMessage<R> deviceMessage) {
        if (Objects.isNull(deviceMessage)) {
            return Flux.error(() -> new BusinessException.NoStackTrace("error.unsupported_property_format"));
        }
        if (deviceMessage.getDeviceId() == null) {
            return Flux.error(() -> new BusinessException.NoStackTrace("error.deviceId_cannot_be_empty"));
        }
        if (StringUtils.isBlank(deviceMessage.getMessageId())) {
            deviceMessage.messageId(IDGenerator.SNOW_FLAKE_STRING.generate());
        }
        return  registry.getDevice(deviceMessage.getDeviceId())
            .switchIfEmpty(deviceInstanceService.handleDeviceNotFoundInRegistry(deviceMessage.getDeviceId()))
            .flatMapMany(deviceOperator -> deviceOperator.rpc().call(deviceMessage));
    }

    /**
     * 按条件查询指定ID设备的属性
     *
     * @return Flux<DeviceProperty>
     */
    @QueryAction
    @org.jetlinks.core.annotation.command.CommandHandler
    public Flux<DeviceProperty> queryPropertiesEach(QueryPropertyEachCommand cmd) {
        List<String> properties = cmd.getProperties();
        String deviceId = cmd.getDeviceId();
        QueryParamEntity queryParam = cmd.asQueryParam();
        String[] propertiesArray = new String[0];
        if (CollectionUtils.isNotEmpty(properties)) {
            propertiesArray = properties.toArray(propertiesArray);
        }
        return deviceDataService.queryEachProperties(deviceId, queryParam, propertiesArray)
            .map(deviceProperty -> FastBeanCopier.copy(deviceProperty, new DeviceProperty()));
    }

    @QueryAction
    @org.jetlinks.core.annotation.command.CommandHandler
    public Flux<DeviceInstanceEntity> queryEntityList(QueryEntityListCommand cmd) {
        return  deviceInstanceService.query(cmd.asQueryParam());
    }

    @QueryAction
    @org.jetlinks.core.annotation.command.CommandHandler
    public Mono<PagerResult<DeviceInstanceEntity>> queryEntityPager(QueryEntityPagerCommand cmd) {
        return  deviceInstanceService.queryPager(cmd.asQueryParam());
    }

    // 激活设备
    @SaveAction
    @org.jetlinks.core.annotation.command.CommandHandler(id = "Enabled", name = "启用", description = "启用或激活")
    public Mono<Void> enableDevice(@DeviceSelector(multiple = true) @Schema(title = "ID") List<String> id) {
        return  deviceInstanceService
            .deploy(deviceInstanceService.findById(id))
            .then();
    }

    // 注销设备
    @SaveAction
    @org.jetlinks.core.annotation.command.CommandHandler(id = "Disabled", name = "禁用", description = "禁用或注销")
    public Mono<Void> disableDevice(@DeviceSelector(multiple = true) @Schema(title = "ID") List<String> id) {
        return deviceInstanceService
            .unregisterDevice(Flux.fromIterable(id))
            .then();
    }

    // 子设备解绑
    @SaveAction
    @org.jetlinks.core.annotation.command.CommandHandler
    public Mono<Void> unbindChildDevice(UnbindChildDeviceCommand cmd) {
        return deviceInstanceService.handleUnbind(cmd.getParentId(), cmd.getDeviceIds());
    }

    @DeleteAction
    @org.jetlinks.core.annotation.command.CommandHandler(id = "DeleteById", name = "根据ID删除")
    public Mono<Integer> deleteById(@DeviceSelector(multiple = true) @Schema(title = "ID") List<String> id) {
        return CrudCommandHandler.super.deleteById(DeleteByIdCommand.of(Integer.class).withIdList(id));
    }

    //设备功能调用
    @org.jetlinks.core.annotation.command.CommandHandler
    public Flux<FunctionInvokeMessageReply> functionInvoke(FunctionInvokeCommand cmd) {
        return this
            .executeDeviceMessageCommand(cmd.getMessage())
            .map(CastUtil::cast);
    }

    //获取设备属性
    @org.jetlinks.core.annotation.command.CommandHandler
    public Flux<ReadPropertyMessageReply> readProperty(ReadPropertyCommand cmd) {
        return this
            .executeDeviceMessageCommand(cmd.getMessage())
            .map(CastUtil::cast);
    }

    //设置设备属性
    @org.jetlinks.core.annotation.command.CommandHandler
    public Flux<WritePropertyMessageReply> writeProperty(WritePropertyCommand cmd) {
        return this
            .executeDeviceMessageCommand(cmd.getMessage())
            .map(CastUtil::cast);
    }

    //发送消息给设备
    @org.jetlinks.core.annotation.command.CommandHandler
    public Flux<? extends ThingMessage> downstream(DownstreamCommand<RepayableDeviceMessage<DeviceMessageReply>, DeviceMessageReply> cmd) {
        return this
            .executeDeviceMessageCommand(cmd.getMessage())
            .map(CastUtil::cast);
    }

    // 创建并启用设备
    @SaveAction
    @org.jetlinks.core.annotation.command.CommandHandler
    public Flux<DeviceInfo> saveAndEnabled(SaveAndEnableCommand<DeviceInfo> cmd) {
        Flux<DeviceInstanceEntity> deviceList = Flux.fromIterable(
            cmd
                .dataList()
                .stream()
                .map(info -> FastBeanCopier.copy(info, DeviceInstanceEntity.class))
                .collect(Collectors.toList())
        );
        return deviceInstanceService
            .save(deviceList)
            .flatMap(i -> deviceInstanceService.deploy(deviceList).then())
            .thenMany(deviceList.map(device -> FastBeanCopier.copy(device, DeviceInfo.class)));
    }

    @org.jetlinks.core.annotation.command.CommandHandler
    @QueryComponent(fields = DeviceInstanceEntity.class)
    @Override
    public Mono<Integer> count(CountCommand command) {
        return CrudCommandHandler.super.count(command);
    }

    @Override
    public ReactiveCrudService<DeviceInstanceEntity, String> getService() {
        return deviceInstanceService;
    }

    @Schema(title = "查询设备实体列表")
    public static class QueryEntityListCommand extends QueryListCommand<DeviceInstanceEntity> {

        public QueryEntityListCommand() {
            withConverter(DeviceCommandSupport::convertEntity);
        }
    }

    @Schema(title = "分页获取设备实体")
    public static class QueryEntityPagerCommand extends QueryPagerCommand<DeviceInstanceEntity> {

        public QueryEntityPagerCommand() {
            withConverter(DeviceCommandSupport::convertEntity);
        }

        public static CommandHandler<QueryEntityPagerCommand, Mono<PagerResult<DeviceInstanceEntity>>>
        createHandler(Function<QueryEntityPagerCommand, Mono<PagerResult<DeviceInstanceEntity>>> handler) {
            return CommandHandler.of(
                () -> {
                    SimpleFunctionMetadata metadata = new SimpleFunctionMetadata();
                    metadata.setId(CommandUtils.getCommandIdByType(QueryEntityPagerCommand.class));
                    metadata.setName("分页获取设备实体");
                    return metadata;
                },
                (cmd, ignore) -> handler.apply(cmd),
                QueryEntityPagerCommand::new
            );
        }
    }

    private static DeviceInstanceEntity convertEntity(Object obj) {
        return obj instanceof DeviceInstanceEntity ? (DeviceInstanceEntity) obj : FastBeanCopier.copy(obj, DeviceInstanceEntity.class);
    }


    /**
     * 获取设备属性实时值
     *
     * @return DeviceProperty
     */
    @org.jetlinks.core.annotation.command.CommandHandler
    @QueryAction
    public Flux<DeviceProperty> queryPropertyRealTime(SubscribeDevicePropertyCommand cmd) {
        List<String> deviceIds = cmd.getDeviceIds();
        String productId = cmd.getProductId();
        List<String> propertyIds = cmd.getPropertyIds();
        String topicDeviceId = StringUtils.join(deviceIds, ",");
        String topicParam = StringUtils.isBlank(topicDeviceId) ? "*" : topicDeviceId;
        String productParam = StringUtils.isBlank(productId) ? "*" : productId;
        String id = IDGenerator.SNOW_FLAKE_STRING.generate();

        String[] topics = {
            "/device/" + productParam + "/" + topicParam + "/message/property/report",
            "/device/" + productParam + "/" + topicParam + "/message/property/*/reply"
        };
        return Authentication
            .currentReactive()
            .doOnNext(auth -> {
                boolean hasPermission = auth.hasPermission(DeviceManagerConstants.Resources.device, Permission.ACTION_QUERY);
                if (!hasPermission) {
                    throw new AccessDenyException.NoStackTrace(DeviceManagerConstants.Resources.device, Collections.singleton(Permission.ACTION_QUERY));
                }
            })
            .flatMapMany(auth -> Flux
                .merge(createSubscribeAssetsHelper(topics[0], auth, String.join(":", "report", id)),
                       createSubscribeAssetsHelper(topics[1], auth, String.join(":", "reply", id)))
                .map(payload -> payload.decode(DeviceMessage.class))
                .flatMap(msg -> Mono
                    .justOrEmpty(DeviceMessageUtils.tryGetProperties(msg))
                    .flatMapMany(map -> registry
                        .getDevice(msg.getDeviceId())
                        .flatMapMany(deviceOperator -> deviceOperator
                            .getMetadata()
                            .flatMapMany(metadata -> Flux
                                .fromIterable(map.entrySet())
                                .mapNotNull(entry -> metadata
                                    .getProperty(entry.getKey())
                                    .map(propertyMetadata -> createValue(entry.getValue(), propertyMetadata, msg.getDeviceId(), msg.getTimestamp()))
                                    .orElse(null)))
                        ))))
            .filter(p -> CollectionUtils.isEmpty(propertyIds) || propertyIds.contains(p.getProperty()));

    }

    /**
     * 构建DeviceProperty值
     *
     * @param value     属性值
     * @param metadata  属性元数据
     * @param deviceId  设备id
     * @param timestamp 时间戳
     * @return DeviceProperty
     */
    private DeviceProperty createValue(Object value, PropertyMetadata metadata, String deviceId, Object timestamp) {
        DeviceProperty deviceProperty = new DeviceProperty();

        DataType type = metadata.getValueType();
        value = type instanceof Converter ? ((Converter<?>) type).convert(value) : value;
        deviceProperty.setDeviceId(deviceId);
        deviceProperty.setProperty(type.getId());
        deviceProperty.setPropertyName(type.getName());
        deviceProperty.setType(type.getType());
        deviceProperty.setValue(value);
        deviceProperty.setFormatValue(type.format(value));
        deviceProperty.setTimestamp(Long.parseLong(String.valueOf(timestamp)));
        if (type instanceof UnitSupported) {
            UnitSupported unitSupported = (UnitSupported) type;
            deviceProperty.setUnit(Optional.ofNullable(unitSupported.getUnit())
                                           .map(ValueUnit::getSymbol)
                                           .orElse(null));
        }
        return deviceProperty;
    }

    private Flux<TopicPayload> subscribe(CharSequence topic, String userId, String id) {
        Subscription subscription = Subscription
            .builder()
            .topics(topic)
            .subscriberId("SubscribeDevicePropertyCommand:" + userId + ":" + id)
            .features(Subscription.Feature.clusterFeatures)
            .build();
        return eventBus
            .subscribe(subscription);
    }

    private Flux<TopicPayload> createSubscribeAssetsHelper(String topic, Authentication authentication, String id) {
        return subscribe(topic, authentication.getUser().getId(), id);
    }

    @Schema(title = "查询设备的指定属性值列表", description = "多个属性逗号隔开")
    public static class QueryDevicePropertyListCommand extends QueryPropertyListCommand {

        @Getter
        @Setter
        protected static class InputSpec extends QueryPropertyListCommand.InputSpec {
            @NotBlank
            @DeviceSelector
            @Schema(title = "设备ID")
            private String deviceId;

            @DevicePropertySelector(deviceIdKey = "deviceId")
            @Schema(title = "属性ID")
            private String property;
        }
    }

    @Schema(title = "分页查询设备的指定属性值", description = "多个属性逗号隔开")
    public static class QueryDeviceQueryPropertyPageCommand extends QueryPropertyPageCommand {

        @Getter
        @Setter
        protected static class InputSpec extends QueryPropertyPageCommand.InputSpec {
            @NotBlank
            @DeviceSelector
            @Schema(title = "设备ID")
            private String deviceId;

            @DevicePropertySelector(deviceIdKey = "deviceId")
            @Schema(title = "属性ID")
            private String property;
        }
    }

}

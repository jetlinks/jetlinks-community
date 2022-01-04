package org.jetlinks.community.device.service;


import org.hswebframework.ezorm.core.StaticMethodReferenceColumn;
import org.hswebframework.ezorm.rdb.mapping.ReactiveRepository;
import org.hswebframework.ezorm.rdb.mapping.ReactiveUpdate;
import org.hswebframework.ezorm.rdb.mapping.defaults.DefaultReactiveRepository;
import org.hswebframework.ezorm.rdb.mapping.defaults.SaveResult;
import org.hswebframework.ezorm.rdb.metadata.RDBDatabaseMetadata;
import org.hswebframework.ezorm.rdb.metadata.RDBTableMetadata;
import org.hswebframework.ezorm.rdb.operator.DatabaseOperator;
import org.hswebframework.ezorm.rdb.operator.dml.upsert.SaveResultOperator;
import org.jetlinks.community.device.entity.DeviceInstanceEntity;
import org.jetlinks.community.device.entity.DeviceProductEntity;
import org.jetlinks.community.device.entity.DeviceStateInfo;
import org.jetlinks.community.device.entity.DeviceTagEntity;
import org.jetlinks.core.defaults.DefaultDeviceOperator;
import org.jetlinks.core.device.*;
import org.jetlinks.core.event.EventBus;
import org.jetlinks.core.event.Subscription;
import org.jetlinks.core.message.*;
import org.jetlinks.core.message.property.ReadPropertyMessage;
import org.jetlinks.supports.config.InMemoryConfigStorage;
import org.jetlinks.supports.config.InMemoryConfigStorageManager;
import org.jetlinks.supports.event.BrokerEventBus;
import org.jetlinks.supports.test.InMemoryDeviceRegistry;
import org.jetlinks.supports.test.MockProtocolSupport;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.reactivestreams.Publisher;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.lang.reflect.Method;
import java.util.*;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class DeviceMessageBusinessHandlerTest {
    public static final String DEVICE_ID = "test001";
    public static final String MESSAGE_ID = "test002";
    public static final String PRODUCT_ID = "test100";

    @Test
    void autoRegisterDevice() {
        LocalDeviceInstanceService deviceService = Mockito.mock(LocalDeviceInstanceService.class);
        LocalDeviceProductService productService = Mockito.mock(LocalDeviceProductService.class);
        ReactiveRepository<DeviceTagEntity, String> tagRepository = Mockito.mock(ReactiveRepository.class);
        //模拟方法调用
        DeviceProductEntity deviceProductEntity = new DeviceProductEntity();
        deviceProductEntity.setId(PRODUCT_ID);
        deviceProductEntity.setMessageProtocol("test");
        deviceProductEntity.setName("test_p");
        deviceProductEntity.setCreatorId("12345678");
        deviceProductEntity.setOrgId("123");
        Mockito.when(productService.findById(Mockito.anyString())).thenReturn(Mono.just(deviceProductEntity));
        SaveResult saveResult = SaveResult.of(1, 0);
        Mockito.when(deviceService.save(Mockito.any(Publisher.class))).thenReturn(Mono.just(saveResult));

        DeviceMessageBusinessHandler service = new DeviceMessageBusinessHandler(deviceService, productService, InMemoryDeviceRegistry.create(), tagRepository, new BrokerEventBus());
        //构造设备注册消息
        DeviceRegisterMessage message = new DeviceRegisterMessage();
        message.setDeviceId("12345");
        message.setMessageId("10000");
//      Class<DeviceRegisterMessage> messageClass = DeviceRegisterMessage.class;
        Map<String, Object> configuration = new HashMap<>();
        configuration.put("test", "test");
        configuration.put(DeviceConfigKey.selfManageState.getKey(), true);
        Map<String, Object> map = new HashMap<>();
        map.put("deviceName", "test");
        map.put("productId", "10001");//产品id
        map.put("configuration", configuration);
        message.setHeaders(map);
        assertNotNull(service);
        //第一次注册，registry.getDevice为空
        service.autoRegisterDevice(message)
            .as(StepVerifier::create)
            .expectSubscription()
            .verifyComplete();
        //第二次注册，registry.getDevice会拿到上一次注册的结果
        service.autoRegisterDevice(message)
            .as(StepVerifier::create)
            .expectSubscription()
            .verifyComplete();

        map.put("configuration", null);
        service.autoRegisterDevice(message)
            .as(StepVerifier::create)
            .expectSubscription()
            .verifyComplete();

    }

    @Test
    void autoRegisterDevice1() {
        LocalDeviceInstanceService deviceService = Mockito.mock(LocalDeviceInstanceService.class);
        LocalDeviceProductService productService = Mockito.mock(LocalDeviceProductService.class);
        ReactiveRepository<DeviceTagEntity, String> tagRepository = Mockito.mock(ReactiveRepository.class);
        //模拟方法调用
        DeviceProductEntity deviceProductEntity = new DeviceProductEntity();
        deviceProductEntity.setId(PRODUCT_ID);
        deviceProductEntity.setMessageProtocol("test");
        deviceProductEntity.setName("test_p");
        deviceProductEntity.setCreatorId("12345678");
        deviceProductEntity.setOrgId("123");
        Mockito.when(productService.findById(Mockito.anyString())).thenReturn(Mono.just(deviceProductEntity));
        SaveResult saveResult = SaveResult.of(1, 0);
        Mockito.when(deviceService.save(Mockito.any(Publisher.class))).thenReturn(Mono.just(saveResult));

        DeviceMessageBusinessHandler service = new DeviceMessageBusinessHandler(deviceService, productService, InMemoryDeviceRegistry.create(), tagRepository, new BrokerEventBus());
        //构造设备注册消息
        DeviceRegisterMessage message = new DeviceRegisterMessage();
        message.setDeviceId("12345");
        message.setMessageId("10000");
        Map<String, Object> configuration = new HashMap<>();
        configuration.put("test", "test");
        configuration.put(DeviceConfigKey.selfManageState.getKey(), false);
        Map<String, Object> map = new HashMap<>();
        map.put("deviceName", "test");
        map.put("productId", "10001");//产品id
        map.put("configuration", configuration);
        message.setHeaders(map);
        assertNotNull(service);
        //第一次注册，registry.getDevice为空 DeviceConfigKey.selfManageState.getKey()为false
        service.autoRegisterDevice(message)
            .as(StepVerifier::create)
            .expectSubscription()
            .verifyComplete();

    }

    @Test
    void autoBindChildrenDevice() {
        LocalDeviceInstanceService deviceService = Mockito.mock(LocalDeviceInstanceService.class);
        LocalDeviceProductService productService = Mockito.mock(LocalDeviceProductService.class);
        ReactiveRepository<DeviceTagEntity, String> tagRepository = Mockito.mock(ReactiveRepository.class);
        //模拟方法调用
        DeviceProductEntity deviceProductEntity = new DeviceProductEntity();
        deviceProductEntity.setId(PRODUCT_ID);
        deviceProductEntity.setMessageProtocol("test");
        deviceProductEntity.setName("test_p");
        deviceProductEntity.setCreatorId("12345678");
        deviceProductEntity.setOrgId("123");
        Mockito.when(productService.findById(Mockito.anyString())).thenReturn(Mono.just(deviceProductEntity));
        SaveResult saveResult = SaveResult.of(1, 0);
        Mockito.when(deviceService.save(Mockito.any(Publisher.class))).thenReturn(Mono.just(saveResult));
        ReactiveUpdate<DeviceInstanceEntity> update = Mockito.mock(ReactiveUpdate.class);
        Mockito.when(deviceService.createUpdate()).thenReturn(update);
        Mockito.when(update.set(Mockito.any(StaticMethodReferenceColumn.class), Mockito.any(Object.class)))
            .thenReturn(update);
        Mockito.when(update.where(Mockito.any(StaticMethodReferenceColumn.class), Mockito.any(Object.class)))
            .thenReturn(update);
        Mockito.when(update.execute())
            .thenReturn(Mono.just(1));

        DeviceMessageBusinessHandler service = new DeviceMessageBusinessHandler(deviceService, productService, InMemoryDeviceRegistry.create(), tagRepository, new BrokerEventBus());

        DeviceRegisterMessage deviceRegisterMessage = new DeviceRegisterMessage();
        deviceRegisterMessage.setDeviceId(DEVICE_ID);
        deviceRegisterMessage.setMessageId("10000");
        Map<String, Object> configuration = new HashMap<>();
        configuration.put("test", "test");
        configuration.put(DeviceConfigKey.selfManageState.getKey(), false);
        Map<String, Object> map = new HashMap<>();
        map.put("deviceName", "test");
        map.put("productId", "10001");//产品id
        map.put("configuration", configuration);
        deviceRegisterMessage.setHeaders(map);

        ChildDeviceMessage childDeviceMessage = ChildDeviceMessage.create(MESSAGE_ID, deviceRegisterMessage);
        //第一次走registry.getDevice为空  会走doAutoRegister()方法
        service.autoBindChildrenDevice(childDeviceMessage)
            .as(StepVerifier::create)
            .expectSubscription()
            .verifyComplete();
        //第二次走registry.getDevice为上一次注册的
        service.autoBindChildrenDevice(childDeviceMessage)
            .as(StepVerifier::create)
            .expectSubscription()
            .verifyComplete();

        //子设备注册消息循环依赖
        deviceRegisterMessage.setDeviceId(MESSAGE_ID);
        service.autoBindChildrenDevice(childDeviceMessage)
            .as(StepVerifier::create)
            .expectSubscription()
            .verifyComplete();
        //消息类型不属于DeviceRegisterMessage
        ChildDeviceMessage childDeviceMessage1 = ChildDeviceMessage.create(MESSAGE_ID, new DeviceLogMessage());
        service.autoBindChildrenDevice(childDeviceMessage1)
            .as(StepVerifier::create)
            .expectSubscription()
            .verifyComplete();
    }

    @Test
    void autoUnbindChildrenDevice() {
        LocalDeviceInstanceService deviceService = Mockito.mock(LocalDeviceInstanceService.class);
        LocalDeviceProductService productService = Mockito.mock(LocalDeviceProductService.class);
        ReactiveRepository<DeviceTagEntity, String> tagRepository = Mockito.mock(ReactiveRepository.class);
        //模拟方法调用
        DeviceProductEntity deviceProductEntity = new DeviceProductEntity();
        deviceProductEntity.setId(PRODUCT_ID);
        deviceProductEntity.setMessageProtocol("test");
        deviceProductEntity.setName("test_p");
        deviceProductEntity.setCreatorId("12345678");
        deviceProductEntity.setOrgId("123");
        Mockito.when(productService.findById(Mockito.anyString())).thenReturn(Mono.just(deviceProductEntity));
        SaveResult saveResult = SaveResult.of(1, 0);
        Mockito.when(deviceService.save(Mockito.any(Publisher.class))).thenReturn(Mono.just(saveResult));
        ReactiveUpdate<DeviceInstanceEntity> update = Mockito.mock(ReactiveUpdate.class);
        Mockito.when(deviceService.createUpdate()).thenReturn(update);
        Mockito.when(update.set(Mockito.any(StaticMethodReferenceColumn.class), Mockito.any(Object.class)))
            .thenReturn(update);
        Mockito.when(update.where(Mockito.any(StaticMethodReferenceColumn.class), Mockito.any(Object.class)))
            .thenReturn(update);
        Mockito.when(update.execute())
            .thenReturn(Mono.just(1));
        InMemoryDeviceRegistry inMemoryDeviceRegistry = InMemoryDeviceRegistry.create();

        DeviceMessageBusinessHandler service = new DeviceMessageBusinessHandler(deviceService, productService, inMemoryDeviceRegistry, tagRepository, new BrokerEventBus());

        DeviceRegisterMessage deviceRegisterMessage = new DeviceRegisterMessage();
        deviceRegisterMessage.setDeviceId(DEVICE_ID);
        deviceRegisterMessage.setMessageId("10000");
        Map<String, Object> configuration = new HashMap<>();
        configuration.put("test", "test");
        configuration.put(DeviceConfigKey.selfManageState.getKey(), false);
//        configuration.put(DeviceConfigKey.parentGatewayId.getKey(),MESSAGE_ID);
        Map<String, Object> map = new HashMap<>();
        map.put("deviceName", "test");
        map.put("productId", "10001");//产品id
        map.put("configuration", configuration);
        deviceRegisterMessage.setHeaders(map);

        ChildDeviceMessage childDeviceMessage = ChildDeviceMessage.create(MESSAGE_ID, deviceRegisterMessage);
        //第一次走registry.getDevice为空  会走doAutoRegister()方法
        service.autoBindChildrenDevice(childDeviceMessage)
            .as(StepVerifier::create)
            .expectSubscription()
            .verifyComplete();
        //第二次走registry.getDevice为上一次注册的
        service.autoBindChildrenDevice(childDeviceMessage)
            .as(StepVerifier::create)
            .expectSubscription()
            .verifyComplete();

        //不是DeviceUnRegisterMessage消息
        service.autoUnbindChildrenDevice(childDeviceMessage)
            .as(StepVerifier::create)
            .expectSubscription()
            .verifyComplete();
        //前面两次操作是注册消息，为下面注销提供条件
        DeviceUnRegisterMessage deviceUnRegisterMessage = new DeviceUnRegisterMessage();
        deviceUnRegisterMessage.setDeviceId(DEVICE_ID);
        childDeviceMessage.setChildDeviceMessage(deviceUnRegisterMessage);
        Mockito.when(update.setNull(Mockito.any(StaticMethodReferenceColumn.class))).thenReturn(update);
        service.autoUnbindChildrenDevice(childDeviceMessage)
            .as(StepVerifier::create)
            .expectSubscription()
            .verifyComplete();

    }

    @Test
    void unRegisterDevice() {
        LocalDeviceInstanceService deviceService = Mockito.mock(LocalDeviceInstanceService.class);
        LocalDeviceProductService productService = Mockito.mock(LocalDeviceProductService.class);
        ReactiveRepository<DeviceTagEntity, String> tagRepository = Mockito.mock(ReactiveRepository.class);

        Mockito.when(deviceService.unregisterDevice(Mockito.anyString())).thenReturn(Mono.just(1));

        DeviceMessageBusinessHandler service = new DeviceMessageBusinessHandler(deviceService, productService, InMemoryDeviceRegistry.create(), tagRepository, new BrokerEventBus());

        DeviceUnRegisterMessage deviceUnRegisterMessage = new DeviceUnRegisterMessage();
        deviceUnRegisterMessage.setDeviceId(DEVICE_ID);
        assertNotNull(service);
        service.unRegisterDevice(deviceUnRegisterMessage)
            .as(StepVerifier::create)
            .expectSubscription()
            .verifyComplete();
    }


    @Test
    void updateDeviceTag() throws Exception {
        LocalDeviceInstanceService deviceService = Mockito.mock(LocalDeviceInstanceService.class);
        LocalDeviceProductService productService = Mockito.mock(LocalDeviceProductService.class);

        DeviceRegistry registry = Mockito.mock(DeviceRegistry.class);

        DeviceInstanceEntity instance = new DeviceInstanceEntity();
        instance.setId(DEVICE_ID);
        instance.setName("TEST");
        instance.setProductId(PRODUCT_ID);
        instance.setProductName("test");
        instance.setConfiguration(new HashMap<>());
        instance.setCreateTimeNow();
        instance.setCreatorId("1234");
        instance.setOrgId("123");
        InMemoryConfigStorage inMemoryConfigStorage = new InMemoryConfigStorage();
        //inMemoryConfigStorage.setConfig(DeviceConfigKey.metadata.getKey(), "{'test':'test'}");//用于加载物理模型时的数据
        inMemoryConfigStorage.setConfig(DeviceConfigKey.protocol.getKey(), "test");
        inMemoryConfigStorage.setConfig(DeviceConfigKey.productId.getKey(), PRODUCT_ID);

        InMemoryConfigStorageManager inMemoryConfigStorageManager = Mockito.mock(InMemoryConfigStorageManager.class);
        Mockito.when(inMemoryConfigStorageManager.getStorage(Mockito.anyString()))
            .thenReturn(Mono.just(inMemoryConfigStorage));
        DeviceRegistry inMemoryDeviceRegistry = new InMemoryDeviceRegistry();
        DeviceProductEntity deviceProductEntity = new DeviceProductEntity();
        deviceProductEntity.setId(PRODUCT_ID);
        deviceProductEntity.setMessageProtocol("test");
        deviceProductEntity.setName("test_p");
        deviceProductEntity.setCreatorId("12345678");
        deviceProductEntity.setOrgId("123");
        deviceProductEntity.setMetadata("{'pr':'pro'}");

        DeviceProductOperator deviceProductOperator = inMemoryDeviceRegistry.register(deviceProductEntity.toProductInfo()).block();
        deviceProductOperator.setConfig(DeviceConfigKey.protocol, "test").subscribe();

        inMemoryDeviceRegistry.register(instance.toDeviceInfo().addConfig("state", DeviceState.online)).subscribe();
        DefaultDeviceOperator defaultDeviceOperator = new DefaultDeviceOperator(DEVICE_ID, new MockProtocolSupport(), inMemoryConfigStorageManager, new StandaloneDeviceMessageBroker(), inMemoryDeviceRegistry);
        defaultDeviceOperator.updateMetadata("{\"events\":[{\"id\":\"fire_alarm\",\"name\":\"火警报警\",\"expands\":{\"level\":\"urgent\"},\"valueType\":{\"type\":\"object\",\"properties\":[{\"id\":\"lat\",\"name\":\"纬度\",\"valueType\":{\"type\":\"float\"}},{\"id\":\"point\",\"name\":\"点位\",\"valueType\":{\"type\":\"int\"}},{\"id\":\"lnt\",\"name\":\"经度\",\"valueType\":{\"type\":\"float\"}}]}}],\"properties\":[{\"id\":\"temperature\",\"name\":\"温度\",\"valueType\":{\"type\":\"float\",\"scale\":2,\"unit\":\"celsiusDegrees\"},\"expands\":{\"readOnly\":\"true\",\"source\":\"device\"}}],\"functions\":[],\"tags\":[{\"id\":\"test\",\"name\":\"tag\",\"valueType\":{\"type\":\"int\",\"unit\":\"meter\"},\"expands\":{\"readOnly\":\"false\"}}]}").subscribe();


        Mockito.when(registry.getDevice(Mockito.any(String.class)))
            .thenReturn(Mono.just(defaultDeviceOperator));
        defaultDeviceOperator.getMetadata()
            .map(e -> e.getTag("test"))
            .map(e -> e.get())
            .map(e -> e.getValueType())
            .subscribe(System.out::println);


        DatabaseOperator databaseOperator = Mockito.mock(DatabaseOperator.class);
        RDBDatabaseMetadata rdbDatabaseMetadata = Mockito.mock(RDBDatabaseMetadata.class);
        Mockito.when(databaseOperator.getMetadata()).thenReturn(rdbDatabaseMetadata);
        Mockito.when(rdbDatabaseMetadata.getTable(Mockito.anyString()))
            .thenReturn(Optional.of(new RDBTableMetadata()));

        DefaultReactiveRepository<DeviceTagEntity, String> tagRepository = Mockito.mock(DefaultReactiveRepository.class);
        SaveResultOperator resultOperator = Mockito.mock(SaveResultOperator.class);
        Class<? extends DefaultReactiveRepository> aClass = tagRepository.getClass();
        Method doSave = aClass.getDeclaredMethod("doSave", Collection.class);
        doSave.setAccessible(true);

        Mockito.when(doSave.invoke(tagRepository,Mockito.any(Collection.class)))
            .thenReturn(resultOperator);
        Mockito.when(resultOperator.reactive())
            .thenReturn(Mono.just(SaveResult.of(1,0)));
        Mockito.when(tagRepository.save(Mockito.any(Publisher.class)))
            .thenCallRealMethod()
            .thenReturn(Mono.just(1));
        DeviceMessageBusinessHandler service = new DeviceMessageBusinessHandler(deviceService, productService, registry, tagRepository, new BrokerEventBus());

        Map<String, Object> map = new HashMap<>();
        map.put("test", "test");
        map.put("test1", "ccc");
        UpdateTagMessage updateTagMessage = new UpdateTagMessage();
        updateTagMessage.setDeviceId(DEVICE_ID);
        updateTagMessage.setTags(map);


        service.updateDeviceTag(updateTagMessage)
            .as(StepVerifier::create)
            .expectSubscription()
            .verifyComplete();


    }


    @Test
    void updateMetadata() {
        LocalDeviceInstanceService deviceService = Mockito.mock(LocalDeviceInstanceService.class);
        LocalDeviceProductService productService = Mockito.mock(LocalDeviceProductService.class);
        ReactiveRepository<DeviceTagEntity, String> tagRepository = Mockito.mock(ReactiveRepository.class);
        DeviceRegistry deviceRegistry = Mockito.mock(DeviceRegistry.class);

        DeviceInstanceEntity instance = new DeviceInstanceEntity();
        instance.setId(DEVICE_ID);
        instance.setName("TEST");
        instance.setProductId(PRODUCT_ID);
        instance.setProductName("test");
        instance.setConfiguration(new HashMap<>());
        instance.setCreateTimeNow();
        instance.setCreatorId("1234");
        instance.setOrgId("123");
        InMemoryConfigStorage inMemoryConfigStorage = new InMemoryConfigStorage();
        inMemoryConfigStorage.setConfig("test", "test");
        inMemoryConfigStorage.setConfig(DeviceConfigKey.metadata.getKey(), "{'test':'test'}");//用于加载物理模型时的数据
        inMemoryConfigStorage.setConfig(DeviceConfigKey.protocol.getKey(), "test");
        inMemoryConfigStorage.setConfig(DeviceConfigKey.productId.getKey(), PRODUCT_ID);

        InMemoryConfigStorageManager inMemoryConfigStorageManager = Mockito.mock(InMemoryConfigStorageManager.class);
        Mockito.when(inMemoryConfigStorageManager.getStorage(Mockito.anyString()))
            .thenReturn(Mono.just(inMemoryConfigStorage));
        DeviceRegistry inMemoryDeviceRegistry = new InMemoryDeviceRegistry();
        inMemoryDeviceRegistry.register(instance.toDeviceInfo().addConfig("state", DeviceState.online)).subscribe();
        DeviceOperator deviceOperator = new DefaultDeviceOperator(DEVICE_ID, new MockProtocolSupport(), inMemoryConfigStorageManager, new StandaloneDeviceMessageBroker(), inMemoryDeviceRegistry);
        deviceOperator.setConfig(DeviceConfigKey.productId.getKey(),"test").subscribe();
        deviceOperator.setConfig(DeviceConfigKey.protocol, "test").subscribe();
        deviceOperator.setConfig("lst_metadata_time", 1L).subscribe();
//        deviceOperator.setConfig(DeviceConfigKey.metadata.getKey(), "{'test':'test'}").subscribe();
        DeviceProductEntity deviceProductEntity = new DeviceProductEntity();
        deviceProductEntity.setId(PRODUCT_ID);
        deviceProductEntity.setMessageProtocol("test");
        deviceProductEntity.setName("test_p");
        deviceProductEntity.setCreatorId("12345678");
        deviceProductEntity.setOrgId("123");
        deviceProductEntity.setMetadata("{'pr':'pro'}");

        //DeviceProductOperator deviceProductOperator = inMemoryDeviceRegistry.register(deviceProductEntity.toProductInfo()).block();
        String s = "{\n" +
            "  \"id\": \"test\",\n" +
            "  \"name\": \"测试\",\n" +
            "  \"properties\": [\n" +
            "    {\n" +
            "      \"id\": \"name\",\n" +
            "      \"name\": \"名称\",\n" +
            "      \"valueType\": {\n" +
            "        \"type\": \"string\"\n" +
            "      }\n" +
            "    }\n" +
            "  ],\n" +
            "  \"functions\": [\n" +
            "    {\n" +
            "      \"id\": \"playVoice\",\n" +
            "      \"name\": \"播放声音\",\n" +
            "      \"inputs\": [\n" +
            "        {\n" +
            "          \"id\": \"text\",\n" +
            "          \"name\": \"文字内容\",\n" +
            "          \"valueType\": {\n" +
            "            \"type\": \"string\"\n" +
            "          }\n" +
            "        }\n" +
            "      ],\n" +
            "      \"output\": {\n" +
            "        \"type\": \"boolean\"\n" +
            "      }\n" +
            "    }\n" +
            "  ],\n" +
            "  \"events\": [\n" +
            "    {\n" +
            "      \"id\": \"temp_sensor\",\n" +
            "      \"name\": \"温度传感器\",\n" +
            "      \"valueType\": {\n" +
            "        \"type\": \"double\"\n" +
            "      }\n" +
            "    },\n" +
            "    {\n" +
            "      \"id\": \"fire_alarm\",\n" +
            "      \"name\": \"火警\",\n" +
            "      \"valueType\": {\n" +
            "        \"type\": \"object\",\n" +
            "        \"properties\": [\n" +
            "          {\n" +
            "            \"id\": \"location\",\n" +
            "            \"name\": \"地点\",\n" +
            "            \"valueType\": {\n" +
            "              \"type\": \"string\"\n" +
            "            }\n" +
            "          },\n" +
            "          {\n" +
            "            \"id\": \"lng\",\n" +
            "            \"name\": \"经度\",\n" +
            "            \"valueType\": {\n" +
            "              \"type\": \"double\"\n" +
            "            }\n" +
            "          },\n" +
            "          {\n" +
            "            \"id\": \"lat\",\n" +
            "            \"name\": \"纬度\",\n" +
            "            \"valueType\": {\n" +
            "              \"type\": \"double\"\n" +
            "            }\n" +
            "          }\n" +
            "        ]\n" +
            "      }\n" +
            "    }\n" +
            "  ]\n" +
            "}";
        deviceOperator.updateMetadata(s).subscribe();
        Mockito.when(deviceRegistry.getDevice(Mockito.anyString()))
            .thenReturn(Mono.just(deviceOperator));

        ReactiveUpdate<DeviceInstanceEntity> update = Mockito.mock(ReactiveUpdate.class);
        Mockito.when(deviceService.createUpdate()).thenReturn(update);
        Mockito.when(update.set(Mockito.any(StaticMethodReferenceColumn.class), Mockito.any(Object.class)))
            .thenReturn(update);
        Mockito.when(update.where(Mockito.any(StaticMethodReferenceColumn.class), Mockito.any(Object.class)))
            .thenReturn(update);
        Mockito.when(update.execute())
            .thenReturn(Mono.just(1));

        DeviceMessageBusinessHandler service = new DeviceMessageBusinessHandler(deviceService, productService, deviceRegistry, tagRepository, new BrokerEventBus());

        DerivedMetadataMessage derivedMetadataMessage = new DerivedMetadataMessage();
        derivedMetadataMessage.setDeviceId(DEVICE_ID);
        derivedMetadataMessage.setAll(true);
        derivedMetadataMessage.setMetadata("{'test':'test'}");
        service.updateMetadata(derivedMetadataMessage)
            .as(StepVerifier::create)
            .expectSubscription()
            .verifyComplete();

        DerivedMetadataMessage derivedMetadataMessage1 = new DerivedMetadataMessage();
        derivedMetadataMessage1.setDeviceId(DEVICE_ID);
        derivedMetadataMessage1.setAll(false);
        derivedMetadataMessage1.setMetadata(s);
        service.updateMetadata(derivedMetadataMessage1)
            .as(StepVerifier::create)
            .expectComplete()
            .verify();
    }

    @Test
    void init() {
        LocalDeviceInstanceService deviceService = Mockito.mock(LocalDeviceInstanceService.class);
        LocalDeviceProductService productService = Mockito.mock(LocalDeviceProductService.class);
        ReactiveRepository<DeviceTagEntity, String> tagRepository = Mockito.mock(ReactiveRepository.class);
        DeviceRegistry registry = Mockito.mock(DeviceRegistry.class);

//        BrokerEventBus brokerEventBus = new BrokerEventBus();
        EventBus eventBus = Mockito.mock(EventBus.class);
        DeviceStateInfo deviceStateInfo = DeviceStateInfo.of(DEVICE_ID, org.jetlinks.community.device.enums.DeviceState.online);
        List<DeviceStateInfo> list = new ArrayList<>();
        list.add(deviceStateInfo);
        Mockito.when(deviceService.syncStateBatch(Mockito.any(Flux.class), Mockito.anyBoolean()))
            .thenReturn(Flux.just(list));
        ReadPropertyMessage message = new ReadPropertyMessage();
        message.setDeviceId("tes");
        Mockito.when(eventBus.subscribe(Mockito.any(Subscription.class),Mockito.any(Class.class)))
            .thenReturn(Flux.just(message));
        DeviceMessageBusinessHandler service = new DeviceMessageBusinessHandler(deviceService, productService, registry, tagRepository, eventBus);
        service.init();


    }
}
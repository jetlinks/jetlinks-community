package org.jetlinks.community.device.message;

import org.hswebframework.ezorm.core.StaticMethodReferenceColumn;
import org.hswebframework.ezorm.rdb.mapping.ReactiveQuery;
import org.hswebframework.ezorm.rdb.mapping.ReactiveRepository;
import org.hswebframework.web.api.crud.entity.QueryParamEntity;
import org.jetlinks.community.device.entity.DeviceInstanceEntity;
import org.jetlinks.community.device.entity.DeviceProductEntity;
import org.jetlinks.community.device.entity.DeviceProperty;
import org.jetlinks.community.device.entity.DeviceTagEntity;
import org.jetlinks.community.device.enums.DeviceState;
import org.jetlinks.community.device.service.data.DeviceDataService;
import org.jetlinks.core.defaults.DefaultDeviceOperator;
import org.jetlinks.core.defaults.DefaultDeviceProductOperator;
import org.jetlinks.core.device.*;
import org.jetlinks.core.message.DeviceDataManager;
import org.jetlinks.core.message.DeviceMessage;
import org.jetlinks.core.message.event.EventMessage;
import org.jetlinks.core.message.property.ReadPropertyMessage;
import org.jetlinks.core.message.property.ReadPropertyMessageReply;
import org.jetlinks.supports.config.InMemoryConfigStorageManager;
import org.jetlinks.supports.event.BrokerEventBus;
import org.jetlinks.supports.test.InMemoryDeviceRegistry;
import org.jetlinks.supports.test.MockProtocolSupport;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.function.*;

import java.util.Map;

import static org.jetlinks.core.device.DeviceConfigKey.productId;
import static org.junit.jupiter.api.Assertions.*;

class DefaultDeviceDataManagerTest {
    public static final String DEVICE_ID = "test001";
    public static final String PRODUCT_ID = "test100";

    @Test
    void newCache() {
        Map<Object, Object> map = DefaultDeviceDataManager.newCache();
        System.out.println(map);
    }

    @Test
    void getLastProperty() {
        DeviceRegistry registry = Mockito.mock(DeviceRegistry.class);
        DeviceDataService dataService = Mockito.mock(DeviceDataService.class);

        ReactiveRepository<DeviceTagEntity, String> tagRepository = Mockito.mock(ReactiveRepository.class);

        DeviceProperty deviceProperty = new DeviceProperty();
        deviceProperty.setValue(DefaultDeviceDataManager.NULL);
        deviceProperty.setTimestamp(System.currentTimeMillis());
        deviceProperty.setState("100");
        Mockito.when(dataService.queryProperty(Mockito.anyString(), Mockito.any(QueryParamEntity.class), Mockito.anyString()))
            .thenReturn(Flux.just(deviceProperty));

        DefaultDeviceDataManager manager = new DefaultDeviceDataManager(registry, dataService, new BrokerEventBus(), tagRepository);
        manager.getLastProperty(DEVICE_ID, DEVICE_ID)
            .map(DeviceDataManager.PropertyValue::getValue)
            .as(StepVerifier::create)
            .expectNext(DefaultDeviceDataManager.NULL)
            .verifyComplete();

        manager.getLastProperty(DEVICE_ID, DEVICE_ID)
            .map(DeviceDataManager.PropertyValue::getValue)
            .as(StepVerifier::create)
            .expectSubscription()
            .verifyComplete();
    }

    @Test
    void getLastProperty1() {
        DeviceRegistry registry = Mockito.mock(DeviceRegistry.class);
        DeviceDataService dataService = Mockito.mock(DeviceDataService.class);

        ReactiveRepository<DeviceTagEntity, String> tagRepository = Mockito.mock(ReactiveRepository.class);

        DeviceProperty deviceProperty = new DeviceProperty();
        deviceProperty.setValue("test");
        deviceProperty.setTimestamp(System.currentTimeMillis());
        deviceProperty.setState("100");
        Mockito.when(dataService.queryProperty(Mockito.anyString(), Mockito.any(QueryParamEntity.class), Mockito.anyString()))
            .thenReturn(Flux.just(deviceProperty));

        DefaultDeviceDataManager manager = new DefaultDeviceDataManager(registry, dataService, new BrokerEventBus(), tagRepository);
        manager.getLastProperty("test1", "test1")
            .map(DeviceDataManager.PropertyValue::getValue)
            .as(StepVerifier::create)
            .expectNext("test")
            .verifyComplete();

        manager.getLastProperty("test1", "test1")
            .map(DeviceDataManager.PropertyValue::getValue)
            .as(StepVerifier::create)
            .expectNext("test")
            .verifyComplete();

        DeviceProperty deviceProperty1 = new DeviceProperty();
        deviceProperty1.setValue("test");
        deviceProperty1.setTimestamp(System.currentTimeMillis() + 40000000000L);
        deviceProperty1.setState("100");
        Mockito.when(dataService.queryProperty(Mockito.anyString(), Mockito.any(QueryParamEntity.class), Mockito.anyString()))
            .thenReturn(Flux.just(deviceProperty1));
        manager.getLastProperty("test1", "test1")
            .map(DeviceDataManager.PropertyValue::getValue)
            .as(StepVerifier::create)
            .expectNext("test")
            .verifyComplete();
    }

    // DevicePropertyRef类
    @Test
    void getLastProperty2() {
        DeviceDataService dataService = Mockito.mock(DeviceDataService.class);
        long l = System.currentTimeMillis();
        DeviceProperty deviceProperty = new DeviceProperty();
        deviceProperty.setValue("test");
        deviceProperty.setTimestamp(l);
        deviceProperty.setState("100");
        Mockito.when(dataService.queryProperty(Mockito.anyString(), Mockito.any(QueryParamEntity.class), Mockito.anyString()))
            .thenReturn(Flux.just(deviceProperty));
        DefaultDeviceDataManager.DevicePropertyRef devicePropertyRef = new DefaultDeviceDataManager.DevicePropertyRef(DEVICE_ID, new BrokerEventBus(), dataService);

        devicePropertyRef.getLastProperty(DEVICE_ID, l)
            .map(DeviceDataManager.PropertyValue::getValue)
            .as(StepVerifier::create)
            .expectNext("test")
            .verifyComplete();

        devicePropertyRef.getLastProperty(DEVICE_ID, -1)
            .map(DeviceDataManager.PropertyValue::getValue)
            .as(StepVerifier::create)
            .expectNext("test")
            .verifyComplete();

        devicePropertyRef.getLastProperty(DEVICE_ID, 1).subscribe();
    }

    @Test
    void getLastProperty3() {

        DeviceDataService dataService = Mockito.mock(DeviceDataService.class);
        Mockito.when(dataService.queryProperty(Mockito.anyString(), Mockito.any(QueryParamEntity.class), Mockito.anyString()))
            .thenReturn(Flux.empty());
        DefaultDeviceDataManager.DevicePropertyRef devicePropertyRef = new DefaultDeviceDataManager.DevicePropertyRef(DEVICE_ID, new BrokerEventBus(), dataService);

        devicePropertyRef.getLastProperty(DEVICE_ID, 1L)
            .map(DeviceDataManager.PropertyValue::getValue)
            .as(StepVerifier::create)
            .expectSubscription()
            .verifyComplete();

    }

    @Test
    void testGetLastProperty() {
        DeviceRegistry registry = Mockito.mock(DeviceRegistry.class);
        DeviceDataService dataService = Mockito.mock(DeviceDataService.class);

        ReactiveRepository<DeviceTagEntity, String> tagRepository = Mockito.mock(ReactiveRepository.class);
        long l = System.currentTimeMillis();
        DeviceProperty deviceProperty = new DeviceProperty();
        deviceProperty.setValue("test");
        deviceProperty.setTimestamp(l);
        deviceProperty.setState("100");
        Mockito.when(dataService.queryProperty(Mockito.anyString(), Mockito.any(QueryParamEntity.class), Mockito.anyString()))
            .thenReturn(Flux.just(deviceProperty));

        DefaultDeviceDataManager manager = new DefaultDeviceDataManager(registry, dataService, new BrokerEventBus(), tagRepository);
        manager.getLastProperty(DEVICE_ID, DEVICE_ID, l)
            .map(DeviceDataManager.PropertyValue::getValue)
            .as(StepVerifier::create)
            .expectNext("test")
            .verifyComplete();

        manager.getLastProperty(DEVICE_ID, DEVICE_ID, -1)
            .map(DeviceDataManager.PropertyValue::getValue)
            .as(StepVerifier::create)
            .expectNext("test")
            .verifyComplete();

    }

    @Test
    void getLastPropertyTime() {
        DeviceRegistry registry = Mockito.mock(DeviceRegistry.class);
        DeviceDataService dataService = Mockito.mock(DeviceDataService.class);

        ReactiveRepository<DeviceTagEntity, String> tagRepository = Mockito.mock(ReactiveRepository.class);
        DefaultDeviceDataManager manager = new DefaultDeviceDataManager(registry, dataService, new BrokerEventBus(), tagRepository);

        long l = System.currentTimeMillis();
        DeviceProperty deviceProperty = new DeviceProperty();
        deviceProperty.setValue("test");
        deviceProperty.setTimestamp(l);
        deviceProperty.setState("100");
        Mockito.when(dataService.queryProperty(Mockito.anyString(), Mockito.any(QueryParamEntity.class)))
            .thenReturn(Flux.just(deviceProperty));

        manager.getLastPropertyTime(DEVICE_ID, l)
            .as(StepVerifier::create)
            .expectNext(l)
            .verifyComplete();

        manager.getLastPropertyTime(DEVICE_ID, l + 1L)
            .as(StepVerifier::create)
            .expectNext(l)
            .verifyComplete();

        long l1 = System.currentTimeMillis();
        DeviceProperty deviceProperty1 = new DeviceProperty();
        deviceProperty1.setValue("test1");
        deviceProperty1.setTimestamp(l1);
        deviceProperty1.setState("1001");
        Mockito.when(dataService.queryProperty(Mockito.anyString(), Mockito.any(QueryParamEntity.class)))
            .thenReturn(Flux.just(deviceProperty1));
        manager.getLastPropertyTime(DEVICE_ID, l - 1L)
            .as(StepVerifier::create)
            .expectNext(l1)
            .verifyComplete();
    }

    @Test
    void getLastPropertyTime1() {
        DeviceRegistry registry = Mockito.mock(DeviceRegistry.class);
        DeviceDataService dataService = Mockito.mock(DeviceDataService.class);

        ReactiveRepository<DeviceTagEntity, String> tagRepository = Mockito.mock(ReactiveRepository.class);
        DefaultDeviceDataManager manager = new DefaultDeviceDataManager(registry, dataService, new BrokerEventBus(), tagRepository);


        Mockito.when(dataService.queryProperty(Mockito.anyString(), Mockito.any(QueryParamEntity.class)))
            .thenReturn(Flux.empty());
        manager.getLastPropertyTime(DEVICE_ID, -1L)
            .as(StepVerifier::create)
            .expectSubscription()
            .verifyComplete();

        manager.getLastPropertyTime(DEVICE_ID, -1L)
            .as(StepVerifier::create)
            .expectSubscription()
            .verifyComplete();
    }

    @Test
    void getFirstPropertyTime() {
        DeviceRegistry registry = Mockito.mock(DeviceRegistry.class);
        DeviceDataService dataService = Mockito.mock(DeviceDataService.class);

        ReactiveRepository<DeviceTagEntity, String> tagRepository = Mockito.mock(ReactiveRepository.class);

        DeviceInstanceEntity deviceInstanceEntity = new DeviceInstanceEntity();
        deviceInstanceEntity.setId(DEVICE_ID);
        deviceInstanceEntity.setState(DeviceState.online);
        deviceInstanceEntity.setCreatorName("超级管理员");
        deviceInstanceEntity.setName("TCP-setvice");
        deviceInstanceEntity.setProductId(PRODUCT_ID);
        deviceInstanceEntity.setProductName("TCP测试");
        deviceInstanceEntity.setDeriveMetadata(
            "{\"events\":[{\"id\":\"fire_alarm\",\"name\":\"火警报警\",\"expands\":{\"level\":\"urgent\"},\"valueType\":{\"type\":\"object\",\"properties\":[{\"id\":\"lat\",\"name\":\"纬度\",\"valueType\":{\"type\":\"float\"}},{\"id\":\"point\",\"name\":\"点位\",\"valueType\":{\"type\":\"int\"}},{\"id\":\"lnt\",\"name\":\"经度\",\"valueType\":{\"type\":\"float\"}}]}}],\"properties\":[{\"id\":\"temperature\",\"name\":\"温度\",\"valueType\":{\"type\":\"float\",\"scale\":2,\"unit\":\"celsiusDegrees\"},\"expands\":{\"readOnly\":\"true\",\"source\":\"device\"}}],\"functions\":[],\"tags\":[{\"id\":\"test\",\"name\":\"tag\",\"valueType\":{\"type\":\"int\",\"unit\":\"meter\"},\"expands\":{\"readOnly\":\"false\"}}]}"
        );


        InMemoryDeviceRegistry inMemoryDeviceRegistry = InMemoryDeviceRegistry.create();
        DeviceOperator deviceOperator = inMemoryDeviceRegistry.register(deviceInstanceEntity.toDeviceInfo()).block();
        deviceOperator.setConfig(DeviceConfigKey.firstPropertyTime, 100L).subscribe();
        Mockito.when(registry.getDevice(Mockito.anyString()))
            .thenReturn(Mono.just(deviceOperator));

        DefaultDeviceDataManager manager = new DefaultDeviceDataManager(registry, dataService, new BrokerEventBus(), tagRepository);

        manager.getFirstPropertyTime(DEVICE_ID)
            .as(StepVerifier::create)
            .expectNext(100L)
            .verifyComplete();

    }

    @Test
    void getFistProperty() {
        DeviceRegistry registry = Mockito.mock(DeviceRegistry.class);
        DeviceDataService dataService = Mockito.mock(DeviceDataService.class);

        ReactiveRepository<DeviceTagEntity, String> tagRepository = Mockito.mock(ReactiveRepository.class);


        DefaultDeviceDataManager manager = new DefaultDeviceDataManager(registry, dataService, new BrokerEventBus(), tagRepository);

        long l = System.currentTimeMillis();
        DeviceProperty deviceProperty = new DeviceProperty();
        deviceProperty.setValue("test");
        deviceProperty.setTimestamp(l);
        deviceProperty.setState("100");
        Mockito.when(dataService.queryProperty(Mockito.anyString(), Mockito.any(QueryParamEntity.class)))
            .thenReturn(Flux.just(deviceProperty));
        manager.getFirstProperty(DEVICE_ID, DEVICE_ID)
            .map(DeviceDataManager.PropertyValue::getValue)
            .as(StepVerifier::create)
            .expectNext("test")
            .verifyComplete();

        manager.getFirstProperty(DEVICE_ID, DEVICE_ID)
            .map(DeviceDataManager.PropertyValue::getValue)
            .as(StepVerifier::create)
            .expectNext("test")
            .verifyComplete();
    }

    @Test
    void getFistProperty1() {
        DeviceRegistry registry = Mockito.mock(DeviceRegistry.class);
        DeviceDataService dataService = Mockito.mock(DeviceDataService.class);

        ReactiveRepository<DeviceTagEntity, String> tagRepository = Mockito.mock(ReactiveRepository.class);


        DefaultDeviceDataManager manager = new DefaultDeviceDataManager(registry, dataService, new BrokerEventBus(), tagRepository);

        long l = System.currentTimeMillis();
        DeviceProperty deviceProperty = new DeviceProperty();
        deviceProperty.setValue(DefaultDeviceDataManager.NULL);
        deviceProperty.setTimestamp(l);
        deviceProperty.setState("100");
        Mockito.when(dataService.queryProperty(Mockito.anyString(), Mockito.any(QueryParamEntity.class)))
            .thenReturn(Flux.just(deviceProperty));
        manager.getFirstProperty(DEVICE_ID, DEVICE_ID)
            .map(DeviceDataManager.PropertyValue::getValue)
            .as(StepVerifier::create)
            .expectNext(DefaultDeviceDataManager.NULL)
            .verifyComplete();

        manager.getFirstProperty(DEVICE_ID, DEVICE_ID)
            .as(StepVerifier::create)
            .expectSubscription()
            .verifyComplete();
    }

    @Test
    void getFistProperty2() {
        DeviceRegistry registry = Mockito.mock(DeviceRegistry.class);
        DeviceDataService dataService = Mockito.mock(DeviceDataService.class);

        ReactiveRepository<DeviceTagEntity, String> tagRepository = Mockito.mock(ReactiveRepository.class);


        DefaultDeviceDataManager manager = new DefaultDeviceDataManager(registry, dataService, new BrokerEventBus(), tagRepository);
        Mockito.when(dataService.queryProperty(Mockito.anyString(), Mockito.any(QueryParamEntity.class)))
            .thenReturn(Flux.empty());

        manager.getFirstProperty(DEVICE_ID, DEVICE_ID)
            .as(StepVerifier::create)
            .expectSubscription()
            .verifyComplete();
    }

    @Test
    void getTags() {

        DeviceRegistry registry = Mockito.mock(DeviceRegistry.class);
        DeviceDataService dataService = Mockito.mock(DeviceDataService.class);

        ReactiveRepository<DeviceTagEntity, String> tagRepository = Mockito.mock(ReactiveRepository.class);

        DeviceInstanceEntity deviceInstanceEntity = new DeviceInstanceEntity();
        deviceInstanceEntity.setId(DEVICE_ID);
        deviceInstanceEntity.setState(DeviceState.online);
        deviceInstanceEntity.setCreatorName("超级管理员");
        deviceInstanceEntity.setName("TCP-setvice");
        deviceInstanceEntity.setProductId(PRODUCT_ID);
        deviceInstanceEntity.setProductName("TCP测试");
        deviceInstanceEntity.setDeriveMetadata(
            "{\"events\":[{\"id\":\"fire_alarm\",\"name\":\"火警报警\",\"expands\":{\"level\":\"urgent\"},\"valueType\":{\"type\":\"object\",\"properties\":[{\"id\":\"lat\",\"name\":\"纬度\",\"valueType\":{\"type\":\"float\"}},{\"id\":\"point\",\"name\":\"点位\",\"valueType\":{\"type\":\"int\"}},{\"id\":\"lnt\",\"name\":\"经度\",\"valueType\":{\"type\":\"float\"}}]}}],\"properties\":[{\"id\":\"temperature\",\"name\":\"温度\",\"valueType\":{\"type\":\"float\",\"scale\":2,\"unit\":\"celsiusDegrees\"},\"expands\":{\"readOnly\":\"true\"}}],\"functions\":[],\"tags\":[{\"id\":\"test\",\"name\":\"tag\",\"valueType\":{\"type\":\"int\",\"unit\":\"meter\"},\"expands\":{\"readOnly\":\"false\"}}]}");

        InMemoryDeviceRegistry inMemoryDeviceRegistry = InMemoryDeviceRegistry.create();

        DeviceInfo deviceInfo = deviceInstanceEntity.toDeviceInfo();
        deviceInfo.addConfig(DeviceConfigKey.protocol, "test")
            .addConfig(DeviceConfigKey.firstPropertyTime, 100L)
            .addConfig(productId.getKey(), "productId")
            .addConfig("lst_metadata_time", 1L);
        DeviceOperator deviceOperator = inMemoryDeviceRegistry.register(deviceInfo).block();
        assertNotNull(deviceOperator);

        Mockito.when(registry.getDevice(Mockito.anyString()))
            .thenReturn(Mono.just(deviceOperator));

        ReactiveQuery<DeviceTagEntity> query = Mockito.mock(ReactiveQuery.class);
        DeviceTagEntity deviceTagEntity = new DeviceTagEntity();
        deviceTagEntity.setId("test");
        deviceTagEntity.setValue("test");
        deviceTagEntity.setKey("test");
        deviceTagEntity.setDeviceId(DEVICE_ID);
        Mockito.when(tagRepository.createQuery())
            .thenReturn(query);
        Mockito.when(query.where(Mockito.any(StaticMethodReferenceColumn.class), Mockito.any(Object.class)))
            .thenReturn(query);
        Mockito.when(query.in(Mockito.any(StaticMethodReferenceColumn.class), Mockito.any(Object.class),Mockito.any(Object.class)))
            .thenReturn(query);
        Mockito.when(query.when(Mockito.any(boolean.class), Mockito.any(Consumer.class)))
            .thenReturn(query)
            .thenCallRealMethod();

        Mockito.when(query.fetch())
            .thenReturn(Flux.just(deviceTagEntity));


        DefaultDeviceDataManager manager = new DefaultDeviceDataManager(registry, dataService, new BrokerEventBus(), tagRepository);

        manager.getTags(DEVICE_ID, "test","test1")
            .map(DeviceDataManager.TagValue::getTagId)
            .as(StepVerifier::create)
            .expectNext("test")
            .verifyComplete();


        InMemoryDeviceRegistry inMemoryDeviceRegistry1 = InMemoryDeviceRegistry.create();

        DeviceOperator deviceOperator1 = inMemoryDeviceRegistry1.register(deviceInfo).block();
        deviceOperator1.updateMetadata("{\"events\":[{\"id\":\"fire_alarm\",\"name\":\"火警报警\",\"expands\":{\"level\":\"urgent\"},\"valueType\":{\"type\":\"object\",\"properties\":[{\"id\":\"lat\",\"name\":\"纬度\",\"valueType\":{\"type\":\"float\"}},{\"id\":\"point\",\"name\":\"点位\",\"valueType\":{\"type\":\"int\"}},{\"id\":\"lnt\",\"name\":\"经度\",\"valueType\":{\"type\":\"float\"}}]}}],\"properties\":[{\"id\":\"temperature\",\"name\":\"温度\",\"valueType\":{\"type\":\"float\",\"scale\":2,\"unit\":\"celsiusDegrees\"},\"expands\":{\"readOnly\":\"true\"}}],\"functions\":[],\"tags\":[]}").subscribe();
        Mockito.when(registry.getDevice(Mockito.anyString()))
            .thenReturn(Mono.just(deviceOperator1));

        manager.getTags(DEVICE_ID, "test")
            .map(DeviceDataManager.TagValue::getTagId)
            .as(StepVerifier::create)
            .expectNext("test")
            .verifyComplete();
    }

    @Test
    void upgradeDeviceFirstPropertyTime() {
        DeviceRegistry registry = Mockito.mock(DeviceRegistry.class);
        DeviceDataService dataService = Mockito.mock(DeviceDataService.class);

        ReactiveRepository<DeviceTagEntity, String> tagRepository = Mockito.mock(ReactiveRepository.class);
        DeviceInstanceEntity deviceInstanceEntity = new DeviceInstanceEntity();
        deviceInstanceEntity.setId(DEVICE_ID);
        deviceInstanceEntity.setState(DeviceState.online);
        deviceInstanceEntity.setCreatorName("超级管理员");
        deviceInstanceEntity.setName("TCP-setvice");
        deviceInstanceEntity.setProductId(PRODUCT_ID);
        deviceInstanceEntity.setProductName("TCP测试");
        deviceInstanceEntity.setDeriveMetadata(
            "{\"events\":[{\"id\":\"fire_alarm\",\"name\":\"火警报警\",\"expands\":{\"level\":\"urgent\"},\"valueType\":{\"type\":\"object\",\"properties\":[{\"id\":\"lat\",\"name\":\"纬度\",\"valueType\":{\"type\":\"float\"}},{\"id\":\"point\",\"name\":\"点位\",\"valueType\":{\"type\":\"int\"}},{\"id\":\"lnt\",\"name\":\"经度\",\"valueType\":{\"type\":\"float\"}}]}}],\"properties\":[{\"id\":\"temperature\",\"name\":\"温度\",\"valueType\":{\"type\":\"float\",\"scale\":2,\"unit\":\"celsiusDegrees\"},\"expands\":{\"readOnly\":\"true\"}}],\"functions\":[],\"tags\":[{\"id\":\"test\",\"name\":\"tag\",\"valueType\":{\"type\":\"int\",\"unit\":\"meter\"},\"expands\":{\"readOnly\":\"false\"}}]}");

        InMemoryDeviceRegistry inMemoryDeviceRegistry = InMemoryDeviceRegistry.create();
        DeviceOperator deviceOperator = inMemoryDeviceRegistry.register(deviceInstanceEntity.toDeviceInfo()).block();
        Mockito.when(registry.getDevice(Mockito.anyString()))
            .thenReturn(Mono.just(deviceOperator));


        DefaultDeviceDataManager manager = new DefaultDeviceDataManager(registry, dataService, new BrokerEventBus(), tagRepository);

        EventMessage eventMessage = new EventMessage();
        eventMessage.setDeviceId(DEVICE_ID);
        manager.upgradeDeviceFirstPropertyTime(eventMessage)
            .as(StepVerifier::create)
            .expectSubscription()
            .verifyComplete();

        manager.upgradeDeviceFirstPropertyTime(eventMessage)
            .as(StepVerifier::create)
            .expectSubscription()
            .verifyComplete();
    }

    // DevicePropertyRef类

    @Test
    void upgrade() throws Exception {

        DeviceDataService dataService = Mockito.mock(DeviceDataService.class);
        DefaultDeviceDataManager.DevicePropertyRef devicePropertyRef = new DefaultDeviceDataManager.DevicePropertyRef(DEVICE_ID, new BrokerEventBus(), dataService);
        devicePropertyRef.dispose();

        Class<? extends DefaultDeviceDataManager.DevicePropertyRef> aClass = devicePropertyRef.getClass();
        Method upgrade = aClass.getDeclaredMethod("upgrade", DeviceMessage.class);
        upgrade.setAccessible(true);


        ReadPropertyMessageReply messageReply = new ReadPropertyMessageReply();
        Map<String, Object> properties = new HashMap<>();
        properties.put("test", "test");
        messageReply.setProperties(properties);
        Map<String, Long> propertySourceTimes = new HashMap<>();
        propertySourceTimes.put("test", 100L);
        messageReply.setPropertySourceTimes(propertySourceTimes);
        Map<String, String> propertyStates = new HashMap<>();
        propertyStates.put("test", "test");
        messageReply.setPropertyStates(propertyStates);

        long l = System.currentTimeMillis();
        DeviceProperty deviceProperty = new DeviceProperty();
        deviceProperty.setValue(DefaultDeviceDataManager.NULL);
        deviceProperty.setTimestamp(l);
        deviceProperty.setState("100");
        Mockito.when(dataService.queryProperty(Mockito.anyString(), Mockito.any(QueryParamEntity.class)))
            .thenReturn(Flux.just(deviceProperty));

        devicePropertyRef.getFirstProperty("test");
        upgrade.invoke(devicePropertyRef, messageReply);


    }
}
package org.jetlinks.community.device.service;


import org.hswebframework.ezorm.core.StaticMethodReferenceColumn;
import org.hswebframework.ezorm.rdb.mapping.ReactiveQuery;
import org.hswebframework.web.exception.NotFoundException;
import org.jetlinks.community.device.entity.DeviceInstanceEntity;
import org.jetlinks.community.device.entity.DeviceProductEntity;
import org.jetlinks.core.ProtocolSupports;
import org.jetlinks.core.defaults.CompositeProtocolSupport;
import org.jetlinks.core.defaults.ExpandsConfigMetadataSupplier;
import org.jetlinks.core.defaults.StaticExpandsConfigMetadataSupplier;
import org.jetlinks.core.message.codec.DefaultTransport;
import org.jetlinks.core.metadata.*;
import org.jetlinks.core.metadata.types.StringType;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class DefaultDeviceConfigMetadataSupplierTest {
    public static final String DEVICE_ID = "test001";
    public static final String PRODUCT_ID = "test100";
    public static final String PROTOCOL_ID = "test1000";
    public static final String TransportProtocol = "TCP";
  @Test
  void getDeviceConfigMetadata() {
      LocalDeviceInstanceService instanceService = Mockito.mock(LocalDeviceInstanceService.class);
      LocalDeviceProductService productService = Mockito.mock(LocalDeviceProductService.class);
      ProtocolSupports protocolSupports = Mockito.mock(ProtocolSupports.class);

      ReactiveQuery<DeviceInstanceEntity> query = Mockito.mock(ReactiveQuery.class);

      DeviceInstanceEntity deviceInstanceEntity = new DeviceInstanceEntity();
      deviceInstanceEntity.setId(DEVICE_ID);
      deviceInstanceEntity.setProductId(PRODUCT_ID);
      Mockito.when(instanceService.createQuery())
          .thenReturn(query);
      Mockito.when(query.select(Mockito.any(StaticMethodReferenceColumn.class)))
          .thenReturn(query);
      Mockito.when(query.where(Mockito.any(StaticMethodReferenceColumn.class),Mockito.any(Object.class)))
          .thenReturn(query);
      Mockito.when(query.fetchOne())
          .thenReturn(Mono.just(deviceInstanceEntity));

      DeviceProductEntity deviceProductEntity = new DeviceProductEntity();
      deviceProductEntity.setId(PRODUCT_ID);
      deviceProductEntity.setMessageProtocol(PROTOCOL_ID);
      deviceProductEntity.setTransportProtocol(TransportProtocol);
      Mockito.when(productService.findById(Mockito.anyString()))
          .thenReturn(Mono.just(deviceProductEntity));
      CompositeProtocolSupport support = new CompositeProtocolSupport();
//      DefaultTransport.TCP;
      DefaultConfigMetadata defaultConfigMetadata = new DefaultConfigMetadata();
      defaultConfigMetadata.add("test","test", StringType.GLOBAL, DeviceConfigScope.device);
      support.addConfigMetadata(DefaultTransport.TCP,defaultConfigMetadata);
      Mockito.when(protocolSupports.getProtocol(Mockito.anyString()))
          .thenReturn(Mono.just(support));


      DefaultDeviceConfigMetadataSupplier sevice = new DefaultDeviceConfigMetadataSupplier(instanceService, productService, protocolSupports);
      assertNotNull(sevice);
      sevice.getDeviceConfigMetadata(DEVICE_ID)
          .map(ConfigMetadata::getProperties)
          .map(s->s.get(0))
          .map(ConfigPropertyMetadata::getName)
          .as(StepVerifier::create)
          .expectNext("test")
          .verifyComplete();

      sevice.getDeviceConfigMetadata("")
          .map(ConfigMetadata::getProperties)
          .map(s->s.get(0))
          .map(ConfigPropertyMetadata::getName)
          .switchIfEmpty(Mono.error(new NotFoundException()))
          .onErrorResume(e->Mono.just("设备id不存在"))
          .as(StepVerifier::create)
          .expectNext("设备id不存在")
          .verifyComplete();

  }

  @Test
  void getDeviceConfigMetadataByProductId() {
      LocalDeviceInstanceService instanceService = Mockito.mock(LocalDeviceInstanceService.class);
      LocalDeviceProductService productService = Mockito.mock(LocalDeviceProductService.class);
      ProtocolSupports protocolSupports = Mockito.mock(ProtocolSupports.class);


      DeviceProductEntity deviceProductEntity = new DeviceProductEntity();
      deviceProductEntity.setId(PRODUCT_ID);
      deviceProductEntity.setMessageProtocol(PROTOCOL_ID);
      deviceProductEntity.setTransportProtocol(TransportProtocol);
      Mockito.when(productService.findById(Mockito.anyString()))
          .thenReturn(Mono.just(deviceProductEntity));
      CompositeProtocolSupport support = new CompositeProtocolSupport();
//      DefaultTransport.TCP;
      DefaultConfigMetadata defaultConfigMetadata = new DefaultConfigMetadata();
      defaultConfigMetadata.add("test","test", StringType.GLOBAL, DeviceConfigScope.device);
      support.addConfigMetadata(DefaultTransport.TCP,defaultConfigMetadata);
      Mockito.when(protocolSupports.getProtocol(Mockito.anyString()))
          .thenReturn(Mono.just(support));

      DefaultDeviceConfigMetadataSupplier sevice = new DefaultDeviceConfigMetadataSupplier(instanceService, productService, protocolSupports);
      assertNotNull(sevice);
      sevice.getDeviceConfigMetadataByProductId(PRODUCT_ID)
          .map(ConfigMetadata::getProperties)
          .map(s->s.get(0))
          .map(ConfigPropertyMetadata::getName)
          .as(StepVerifier::create)
          .expectNext("test")
          .verifyComplete();

      sevice.getDeviceConfigMetadataByProductId("")
          .map(ConfigMetadata::getProperties)
          .map(s->s.get(0))
          .map(ConfigPropertyMetadata::getName)
          .switchIfEmpty(Mono.error(new NotFoundException()))
          .onErrorResume(e->Mono.just("产品id不存在"))
          .as(StepVerifier::create)
          .expectNext("产品id不存在")
          .verifyComplete();
  }

  @Test
  void getProductConfigMetadata() {
      LocalDeviceInstanceService instanceService = Mockito.mock(LocalDeviceInstanceService.class);
      LocalDeviceProductService productService = Mockito.mock(LocalDeviceProductService.class);
      ProtocolSupports protocolSupports = Mockito.mock(ProtocolSupports.class);


      DeviceProductEntity deviceProductEntity = new DeviceProductEntity();
      deviceProductEntity.setId(PRODUCT_ID);
      deviceProductEntity.setMessageProtocol(PROTOCOL_ID);
      deviceProductEntity.setTransportProtocol(TransportProtocol);
      Mockito.when(productService.findById(Mockito.anyString()))
          .thenReturn(Mono.just(deviceProductEntity));
      CompositeProtocolSupport support = new CompositeProtocolSupport();
//      DefaultTransport.TCP;
      DefaultConfigMetadata defaultConfigMetadata = new DefaultConfigMetadata();
      defaultConfigMetadata.add("test","test", StringType.GLOBAL, DeviceConfigScope.product);
      support.addConfigMetadata(DefaultTransport.TCP,defaultConfigMetadata);
      Mockito.when(protocolSupports.getProtocol(Mockito.anyString()))
          .thenReturn(Mono.just(support));

      DefaultDeviceConfigMetadataSupplier sevice = new DefaultDeviceConfigMetadataSupplier(instanceService, productService, protocolSupports);
      assertNotNull(sevice);
      sevice.getProductConfigMetadata(PRODUCT_ID)
          .map(ConfigMetadata::getProperties)
          .map(s->s.get(0))
          .map(ConfigPropertyMetadata::getName)
          .as(StepVerifier::create)
          .expectNext("test")
          .verifyComplete();

      sevice.getProductConfigMetadata("")
          .map(ConfigMetadata::getProperties)
          .map(s->s.get(0))
          .map(ConfigPropertyMetadata::getName)
          .switchIfEmpty(Mono.error(new NotFoundException()))
          .onErrorResume(e->Mono.just("产品id不存在"))
          .as(StepVerifier::create)
          .expectNext("产品id不存在")
          .verifyComplete();
  }

  @Test
  void getMetadataExpandsConfig() {
      LocalDeviceInstanceService instanceService = Mockito.mock(LocalDeviceInstanceService.class);
      LocalDeviceProductService productService = Mockito.mock(LocalDeviceProductService.class);
      ProtocolSupports protocolSupports = Mockito.mock(ProtocolSupports.class);
      ReactiveQuery<DeviceProductEntity> query = Mockito.mock(ReactiveQuery.class);

      DeviceProductEntity deviceProductEntity = new DeviceProductEntity();
      deviceProductEntity.setId(PRODUCT_ID);
      deviceProductEntity.setMessageProtocol(PROTOCOL_ID);
      deviceProductEntity.setTransportProtocol(TransportProtocol);
      Mockito.when(productService.createQuery())
          .thenReturn(query);
      Mockito.when(query.select(Mockito.any(StaticMethodReferenceColumn.class),Mockito.any(StaticMethodReferenceColumn.class)))
          .thenReturn(query);
      Mockito.when(query.where(Mockito.any(StaticMethodReferenceColumn.class),Mockito.any(Object.class)))
          .thenReturn(query);
      Mockito.when(query.fetchOne()).thenReturn(Mono.just(deviceProductEntity));

      CompositeProtocolSupport support = new CompositeProtocolSupport();
      DefaultConfigMetadata defaultConfigMetadata = new DefaultConfigMetadata();
      defaultConfigMetadata.add("test","test", StringType.GLOBAL, DeviceConfigScope.product);
      DefaultConfigMetadata defaultConfigMetadata1 = new DefaultConfigMetadata();
      defaultConfigMetadata1.add("test1","test1", StringType.GLOBAL, DeviceConfigScope.product);
      DefaultConfigMetadata defaultConfigMetadata2 = new DefaultConfigMetadata();
      defaultConfigMetadata2.add("test2","test2", StringType.GLOBAL, DeviceConfigScope.product);
      DefaultConfigMetadata defaultConfigMetadata3 = new DefaultConfigMetadata();
      defaultConfigMetadata3.add("test3","test3", StringType.GLOBAL, DeviceConfigScope.product);
      StaticExpandsConfigMetadataSupplier staticExpandsConfigMetadataSupplier = ExpandsConfigMetadataSupplier.create();
      staticExpandsConfigMetadataSupplier.addConfigMetadata(defaultConfigMetadata);//默认 any:any
      staticExpandsConfigMetadataSupplier.addConfigMetadata("test",defaultConfigMetadata1);//数据类型ID
      staticExpandsConfigMetadataSupplier.addConfigMetadata(DeviceMetadataType.property,defaultConfigMetadata2);//物模型类型
      staticExpandsConfigMetadataSupplier.addConfigMetadata(DeviceMetadataType.property,"test",defaultConfigMetadata3);
      //support.addConfigMetadata(DefaultTransport.TCP,defaultConfigMetadata);
      support.setExpandsConfigMetadata(DefaultTransport.TCP, staticExpandsConfigMetadataSupplier);
      Mockito.when(protocolSupports.getProtocol(Mockito.anyString()))
          .thenReturn(Mono.just(support));



      DefaultDeviceConfigMetadataSupplier sevice = new DefaultDeviceConfigMetadataSupplier(instanceService, productService, protocolSupports);
      assertNotNull(sevice);
      sevice.getMetadataExpandsConfig(PRODUCT_ID, DeviceMetadataType.property,"test","test")
          .map(ConfigMetadata::getProperties)
          .map(s->s.get(0))
          .map(ConfigPropertyMetadata::getName)
          .as(StepVerifier::create)
          .expectNext("test")
          .expectNext("test1")
          .expectNext("test2")
          .expectNext("test3")
          .verifyComplete();
  }




}
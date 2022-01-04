package org.jetlinks.community.device.service;


import org.hswebframework.ezorm.core.StaticMethodReferenceColumn;
import org.hswebframework.ezorm.rdb.mapping.ReactiveRepository;
import org.hswebframework.ezorm.rdb.mapping.ReactiveUpdate;
import org.hswebframework.web.exception.NotFoundException;
import org.jetlinks.community.device.entity.ProtocolSupportEntity;
import org.jetlinks.supports.protocol.management.ProtocolSupportDefinition;
import org.jetlinks.supports.protocol.management.ProtocolSupportLoader;
import org.jetlinks.supports.protocol.management.ProtocolSupportManager;
import org.jetlinks.supports.test.MockProtocolSupport;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class LocalProtocolSupportServiceTest {

    public static final String ID_1 = "test001";
    private final ProtocolSupportManager supportManager = Mockito.mock(ProtocolSupportManager.class);
    private final ProtocolSupportLoader loader = Mockito.mock(ProtocolSupportLoader.class);
    private final ReactiveRepository<ProtocolSupportEntity, String> repository = Mockito.mock(ReactiveRepository.class);

    LocalProtocolSupportService getService() {
        Class<? extends LocalProtocolSupportService> serviceClass = LocalProtocolSupportService.class;
        LocalProtocolSupportService service = null;
        try {
            Constructor<LocalProtocolSupportService> constructor = (Constructor<LocalProtocolSupportService>) serviceClass.getConstructor();
            service = constructor.newInstance();
            Field supportManager = serviceClass.getDeclaredField("supportManager");
            supportManager.setAccessible(true);
            supportManager.set(service, this.supportManager);
            Field loader = serviceClass.getDeclaredField("loader");
            loader.setAccessible(true);
            loader.set(service, this.loader);
        } catch (Exception e) {
            e.printStackTrace();
        }

        try {
            Field repository =serviceClass.getSuperclass().getDeclaredField("repository");
            repository.setAccessible(true);
            repository.set(service, this.repository);
        } catch (Exception e) {
            e.printStackTrace();
        }

        return service;
    }

    @Test
    void deploy() {
        //ReactiveRepository<ProtocolSupportEntity, String> repository = Mockito.mock(ReactiveRepository.class);
        ReactiveUpdate<ProtocolSupportEntity> update = Mockito.mock(ReactiveUpdate.class);
        Map<String, Object> map = new HashMap<>();
        map.put("provider", "org.jetlinks.demo.protocol.DemoProtocolSupportProvider");
        ProtocolSupportEntity protocolSupportEntity = new ProtocolSupportEntity();
        protocolSupportEntity.setId(ID_1);
        protocolSupportEntity.setConfiguration(map);
        protocolSupportEntity.setName("test");
        protocolSupportEntity.setDescription("单元测试");
        protocolSupportEntity.setState((byte) 1);
        protocolSupportEntity.setType("jar");

        Mockito.when(repository.findById(Mockito.any(Mono.class))).thenReturn(Mono.just(protocolSupportEntity));

        //ProtocolSupportManager supportManager = Mockito.mock(ProtocolSupportManager.class);
        //ProtocolSupportLoader loader = Mockito.mock(ProtocolSupportLoader.class);


//        Mono load = new MultiProtocolSupportLoader().load(protocolSupportEntity.toDeployDefinition());
        MockProtocolSupport mockProtocolSupport = new MockProtocolSupport();
        Mono protocolSupportMono = Mono.just(mockProtocolSupport);
        Mockito.when(
            loader.load(Mockito.any(ProtocolSupportDefinition.class))
        ).thenReturn(protocolSupportMono);


        Mockito.when(repository.createUpdate()).thenReturn(update);
        Mockito.when(update.set(Mockito.any(StaticMethodReferenceColumn.class), Mockito.any(Object.class))).thenReturn(update);
        Mockito.when(update.where(Mockito.any(StaticMethodReferenceColumn.class), Mockito.any(Object.class))).thenReturn(update);
        Mockito.when(update.execute()).thenReturn(Mono.just(1));

        Mockito.when(supportManager.save(Mockito.any(ProtocolSupportDefinition.class))).thenReturn(Mono.just(true));
//        LocalProtocolSupportService service = new LocalProtocolSupportService(supportManager, loader) {
//            @Override
//            public ReactiveRepository<ProtocolSupportEntity, String> getRepository() {
//                return repository;
//            }
//        };

        LocalProtocolSupportService service = getService();
        assertNotNull(service);
        service.deploy(ID_1)
            .as(StepVerifier::create)
            .expectNext(true)
            .verifyComplete();

        Mockito.when(update.execute()).thenReturn(Mono.just(0));
        service.deploy(ID_1)
            .as(StepVerifier::create)
            .expectNext(false)
            .verifyComplete();

        Mockito.when(
            loader.load(Mockito.any(ProtocolSupportDefinition.class))
        ).thenReturn(Mono.error(new NotFoundException()));
        service.deploy(ID_1)
            .map(Object::toString)
            .onErrorResume(e -> Mono.just(e.getMessage()))
            .as(StepVerifier::create)
            .expectNext("无法加载协议:error.not_found")
            .verifyComplete();


    }


    @Test
    void unDeploy() {
        //ReactiveRepository<ProtocolSupportEntity, String> repository = Mockito.mock(ReactiveRepository.class);
        ReactiveUpdate<ProtocolSupportEntity> update = Mockito.mock(ReactiveUpdate.class);
        Map<String, Object> map = new HashMap<>();
        map.put("provider", "org.jetlinks.demo.protocol.DemoProtocolSupportProvider");
        ProtocolSupportEntity protocolSupportEntity = new ProtocolSupportEntity();
        protocolSupportEntity.setId(ID_1);
        protocolSupportEntity.setConfiguration(map);
        protocolSupportEntity.setName("test");
        protocolSupportEntity.setDescription("单元测试");
        protocolSupportEntity.setState((byte) 1);
        protocolSupportEntity.setType("jar");
        Mockito.when(repository.findById(Mockito.any(Mono.class))).thenReturn(Mono.justOrEmpty(protocolSupportEntity));

        Mockito.when(repository.createUpdate()).thenReturn(update);
        Mockito.when(update.set(Mockito.any(StaticMethodReferenceColumn.class), Mockito.any(Object.class))).thenReturn(update);
        Mockito.when(update.where(Mockito.any(StaticMethodReferenceColumn.class), Mockito.any(Object.class))).thenReturn(update);
        Mockito.when(update.execute()).thenReturn(Mono.just(1));

        //ProtocolSupportManager supportManager = Mockito.mock(ProtocolSupportManager.class);
        //ProtocolSupportLoader loader = Mockito.mock(ProtocolSupportLoader.class);


        Mockito.when(supportManager.save(Mockito.any(ProtocolSupportDefinition.class))).thenReturn(Mono.just(true));
//        LocalProtocolSupportService service = new LocalProtocolSupportService(supportManager, loader) {
//            @Override
//            public ReactiveRepository<ProtocolSupportEntity, String> getRepository() {
//                return repository;
//            }
//        };
        LocalProtocolSupportService service = getService();
        assertNotNull(service);
        service.unDeploy(ID_1)
            .as(StepVerifier::create)
            .expectNext(true)
            .verifyComplete();

        Mockito.when(update.execute()).thenReturn(Mono.just(0));
        service.unDeploy(ID_1)
            .as(StepVerifier::create)
            .expectNext(false)
            .verifyComplete();


    }
}
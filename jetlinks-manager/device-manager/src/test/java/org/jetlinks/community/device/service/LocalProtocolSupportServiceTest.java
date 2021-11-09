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

import java.util.HashMap;
import java.util.Map;

class LocalProtocolSupportServiceTest {

//    @Autowired
//    private ProtocolSupportManager supportManager;
//
//    @Autowired
//    private ProtocolSupportLoader loader;
//
    public static final String ID_1 = "test001";

    @Test
    void deploy() {
        ReactiveRepository<ProtocolSupportEntity, String> repository = Mockito.mock(ReactiveRepository.class);
        ReactiveUpdate<ProtocolSupportEntity> update = Mockito.mock(ReactiveUpdate.class);
        Map<String, Object> map = new HashMap<>();
        map.put("provider","org.jetlinks.demo.protocol.DemoProtocolSupportProvider");
        ProtocolSupportEntity protocolSupportEntity = new ProtocolSupportEntity();
        protocolSupportEntity.setId(ID_1);
        protocolSupportEntity.setConfiguration(map);
        protocolSupportEntity.setName("test");
        protocolSupportEntity.setDescription("单元测试");
        protocolSupportEntity.setState((byte)1);
        protocolSupportEntity.setType("jar");

        Mockito.when(repository.findById(Mockito.any(Mono.class))).thenReturn(Mono.just(protocolSupportEntity));

        ProtocolSupportManager supportManager = Mockito.mock(ProtocolSupportManager.class);
        ProtocolSupportLoader loader = Mockito.mock(ProtocolSupportLoader.class);


//        Mono load = new MultiProtocolSupportLoader().load(protocolSupportEntity.toDeployDefinition());
        MockProtocolSupport mockProtocolSupport = new MockProtocolSupport();
        Mono protocolSupportMono = Mono.just(mockProtocolSupport);
        Mockito.when(
            loader.load(Mockito.any(ProtocolSupportDefinition.class))
        ).thenReturn(protocolSupportMono);


        Mockito.when(repository.createUpdate()).thenReturn(update);
        Mockito.when(update.set(Mockito.any(StaticMethodReferenceColumn.class),Mockito.any(Object.class))).thenReturn(update);
        Mockito.when(update.where(Mockito.any(StaticMethodReferenceColumn.class),Mockito.any(Object.class))).thenReturn(update);
        Mockito.when(update.execute()).thenReturn(Mono.just(1));

        Mockito.when(supportManager.save(Mockito.any(ProtocolSupportDefinition.class))).thenReturn(Mono.just(true));
        LocalProtocolSupportService service = new LocalProtocolSupportService(supportManager,loader) {
            @Override
            public ReactiveRepository<ProtocolSupportEntity, String> getRepository() {
                return repository;
            }
        };

//        service.deploy(ID_1).subscribe(System.out::println);

//       service.findById(Mono.just(ID_1))
//           .map(ProtocolSupportEntity::getConfiguration)
//           .map(m->m.get("provider"))
//           .as(StepVerifier::create)
//           .expectNext("org.jetlinks.demo.protocol.DemoProtocolSupportProvider")
//           .expectComplete()
//           .verify();
//
        service.deploy(ID_1)
            .as(StepVerifier::create)
            .expectNext(true)
            .verifyComplete();

        Mockito.when(update.execute()).thenReturn(Mono.just(0));
        service.deploy(ID_1)
            .as(StepVerifier::create)
            .expectNext(false)
            .verifyComplete();

//        new LocalProtocolSupportService().deploy("test001");

        Mockito.when(
            loader.load(Mockito.any(ProtocolSupportDefinition.class))
        ).thenReturn(Mono.error(new NotFoundException()));
        service.deploy(ID_1)
            .map(Object::toString)
            .onErrorResume(e->Mono.just(e.getMessage()))
            .as(StepVerifier::create)
            .expectNext("无法加载协议:error.not_found")
            .verifyComplete();


    }


    @Test
    void unDeploy() {
        ReactiveRepository<ProtocolSupportEntity, String> repository = Mockito.mock(ReactiveRepository.class);
        ReactiveUpdate<ProtocolSupportEntity> update = Mockito.mock(ReactiveUpdate.class);
        Map<String, Object> map = new HashMap<>();
        map.put("provider","org.jetlinks.demo.protocol.DemoProtocolSupportProvider");
        ProtocolSupportEntity protocolSupportEntity = new ProtocolSupportEntity();
        protocolSupportEntity.setId(ID_1);
        protocolSupportEntity.setConfiguration(map);
        protocolSupportEntity.setName("test");
        protocolSupportEntity.setDescription("单元测试");
        protocolSupportEntity.setState((byte)1);
        protocolSupportEntity.setType("jar");
        Mockito.when(repository.findById(Mockito.any(Mono.class))).thenReturn(Mono.justOrEmpty(protocolSupportEntity));

        Mockito.when(repository.createUpdate()).thenReturn(update);
        Mockito.when(update.set(Mockito.any(StaticMethodReferenceColumn.class),Mockito.any(Object.class))).thenReturn(update);
        Mockito.when(update.where(Mockito.any(StaticMethodReferenceColumn.class),Mockito.any(Object.class))).thenReturn(update);
        Mockito.when(update.execute()).thenReturn(Mono.just(1));

        ProtocolSupportManager supportManager = Mockito.mock(ProtocolSupportManager.class);
        ProtocolSupportLoader loader = Mockito.mock(ProtocolSupportLoader.class);


        Mockito.when(supportManager.save(Mockito.any(ProtocolSupportDefinition.class))).thenReturn(Mono.just(true));
        LocalProtocolSupportService service = new LocalProtocolSupportService(supportManager,loader) {
            @Override
            public ReactiveRepository<ProtocolSupportEntity, String> getRepository() {
                return repository;
            }
        };
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
package org.jetlinks.community.notify.manager.service;

import org.hswebframework.ezorm.core.StaticMethodReferenceColumn;
import org.hswebframework.ezorm.rdb.mapping.ReactiveQuery;
import org.hswebframework.ezorm.rdb.mapping.ReactiveRepository;
import org.hswebframework.ezorm.rdb.mapping.ReactiveUpdate;
import org.hswebframework.web.authorization.Authentication;
import org.hswebframework.web.authorization.ReactiveAuthenticationHolder;
import org.hswebframework.web.authorization.ReactiveAuthenticationSupplier;
import org.hswebframework.web.crud.events.EntityCreatedEvent;
import org.hswebframework.web.crud.events.EntityDeletedEvent;
import org.hswebframework.web.crud.events.EntityModifyEvent;
import org.hswebframework.web.crud.events.EntitySavedEvent;
import org.hswebframework.web.exception.NotFoundException;
import org.jetlinks.community.notify.manager.entity.Notification;
import org.jetlinks.community.notify.manager.entity.NotifySubscriberEntity;
import org.jetlinks.community.notify.manager.enums.SubscribeState;
import org.jetlinks.community.notify.manager.subscriber.SubscriberProvider;
import org.jetlinks.community.notify.manager.subscriber.providers.DeviceAlarmProvider;
import org.jetlinks.community.test.web.TestAuthentication;
import org.jetlinks.core.cluster.ClusterManager;
import org.jetlinks.core.cluster.ClusterTopic;
import org.jetlinks.core.event.EventBus;
import org.jetlinks.supports.event.BrokerEventBus;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.reactivestreams.Publisher;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class NotifySubscriberServiceTest {


    @Test
    void getProvider() {
        ClusterManager clusterManager = Mockito.mock(ClusterManager.class);
        List<SubscriberProvider> providers = new ArrayList<>();
        DeviceAlarmProvider deviceAlarmProvider = new DeviceAlarmProvider(new BrokerEventBus());
        providers.add(deviceAlarmProvider);
        NotifySubscriberService service = new NotifySubscriberService(new BrokerEventBus(), clusterManager, providers);
        String name = service.getProvider("device_alarm")
            .get().getName();
        assertEquals("设备告警",name);

    }

    @Test
    void handleEvent() {
        ClusterManager clusterManager = Mockito.mock(ClusterManager.class);
        List<SubscriberProvider> providers = new ArrayList<>();
        NotifySubscriberService service = new NotifySubscriberService(new BrokerEventBus(), clusterManager, providers);
        List<NotifySubscriberEntity> list = new ArrayList<>();
        NotifySubscriberEntity notifySubscriberEntity = new NotifySubscriberEntity();
        notifySubscriberEntity.setId("test");
        notifySubscriberEntity.setTopicProvider("device_alarm");
        notifySubscriberEntity.setSubscriberType("test");
        notifySubscriberEntity.setSubscriber("test");
        list.add(notifySubscriberEntity);

        ClusterTopic<Object> redisClusterTopic = Mockito.mock(ClusterTopic.class);
        Mockito.when(clusterManager.getTopic(Mockito.anyString()))
            .thenReturn(redisClusterTopic);
        Mockito.when(redisClusterTopic.publish(Mockito.any(Publisher.class)))
            .thenReturn(Mono.just(1));

        assertNotNull(service);
        EntityCreatedEvent<NotifySubscriberEntity> entity = new EntityCreatedEvent<>(list, NotifySubscriberEntity.class);
        service.handleEvent(entity);
    }

    @Test
    void testHandleEvent() {
        ClusterManager clusterManager = Mockito.mock(ClusterManager.class);
        List<SubscriberProvider> providers = new ArrayList<>();
        NotifySubscriberService service = new NotifySubscriberService(new BrokerEventBus(), clusterManager, providers);
        List<NotifySubscriberEntity> list = new ArrayList<>();
        NotifySubscriberEntity notifySubscriberEntity = new NotifySubscriberEntity();
        notifySubscriberEntity.setId("test");
        notifySubscriberEntity.setTopicProvider("device_alarm");
        notifySubscriberEntity.setSubscriberType("test");
        notifySubscriberEntity.setSubscriber("test");
        list.add(notifySubscriberEntity);

        ClusterTopic<Object> redisClusterTopic = Mockito.mock(ClusterTopic.class);
        Mockito.when(clusterManager.getTopic(Mockito.anyString()))
            .thenReturn(redisClusterTopic);
        Mockito.when(redisClusterTopic.publish(Mockito.any(Publisher.class)))
            .thenReturn(Mono.just(1));
        assertNotNull(service);
        EntitySavedEvent<NotifySubscriberEntity> entity = new EntitySavedEvent<>(list, NotifySubscriberEntity.class);
        service.handleEvent(entity);
    }

    @Test
    void testHandleEvent1() {
        ClusterManager clusterManager = Mockito.mock(ClusterManager.class);
        List<SubscriberProvider> providers = new ArrayList<>();
        NotifySubscriberService service = new NotifySubscriberService(new BrokerEventBus(), clusterManager, providers);
        List<NotifySubscriberEntity> list = new ArrayList<>();
        NotifySubscriberEntity notifySubscriberEntity = new NotifySubscriberEntity();
        notifySubscriberEntity.setId("test");
        notifySubscriberEntity.setTopicProvider("device_alarm");
        notifySubscriberEntity.setSubscriberType("test");
        notifySubscriberEntity.setSubscriber("test");
        list.add(notifySubscriberEntity);

        ClusterTopic<Object> redisClusterTopic = Mockito.mock(ClusterTopic.class);
        Mockito.when(clusterManager.getTopic(Mockito.anyString()))
            .thenReturn(redisClusterTopic);
        Mockito.when(redisClusterTopic.publish(Mockito.any(Publisher.class)))
            .thenReturn(Mono.just(1));
        assertNotNull(service);
        EntityDeletedEvent<NotifySubscriberEntity> entity = new EntityDeletedEvent<>(list, NotifySubscriberEntity.class);
        service.handleEvent(entity);
    }

    @Test
    void testHandleEvent2() {
        ClusterManager clusterManager = Mockito.mock(ClusterManager.class);
        List<SubscriberProvider> providers = new ArrayList<>();
        NotifySubscriberService service = new NotifySubscriberService(new BrokerEventBus(), clusterManager, providers);
        List<NotifySubscriberEntity> list = new ArrayList<>();
        NotifySubscriberEntity notifySubscriberEntity = new NotifySubscriberEntity();
        notifySubscriberEntity.setId("test");
        notifySubscriberEntity.setTopicProvider("device_alarm");
        notifySubscriberEntity.setSubscriberType("test");
        notifySubscriberEntity.setSubscriber("test");
        list.add(notifySubscriberEntity);

        ClusterTopic<Object> redisClusterTopic = Mockito.mock(ClusterTopic.class);
        Mockito.when(clusterManager.getTopic(Mockito.anyString()))
            .thenReturn(redisClusterTopic);
        Mockito.when(redisClusterTopic.publish(Mockito.any(Publisher.class)))
            .thenReturn(Mono.just(1));
        assertNotNull(service);
        EntityModifyEvent<NotifySubscriberEntity> entity = new EntityModifyEvent<>(new ArrayList<>(),list, NotifySubscriberEntity.class);
        service.handleEvent(entity);
    }

    @Test
    void doSubscribe() {
        ClusterManager clusterManager = Mockito.mock(ClusterManager.class);
        ReactiveRepository<NotifySubscriberEntity, String> repository = Mockito.mock(ReactiveRepository.class);
        ReactiveUpdate<NotifySubscriberEntity> update = Mockito.mock(ReactiveUpdate.class);
        List<SubscriberProvider> providers = new ArrayList<>();
        DeviceAlarmProvider deviceAlarmProvider = new DeviceAlarmProvider(new BrokerEventBus());
        providers.add(deviceAlarmProvider);
        Mockito.when(repository.createUpdate())
            .thenReturn(update);
        Mockito.when(update.set(Mockito.any(NotifySubscriberEntity.class)))
            .thenReturn(update);
        Mockito.when(update.where(Mockito.any(StaticMethodReferenceColumn.class),Mockito.any(Object.class)))
            .thenReturn(update);
        Mockito.when(update.and(Mockito.any(StaticMethodReferenceColumn.class),Mockito.any(Object.class)))
            .thenReturn(update);
        Mockito.when(update.and(Mockito.any(StaticMethodReferenceColumn.class),Mockito.any(Object.class)))
            .thenReturn(update);
        Mockito.when(update.execute())
            .thenReturn(Mono.just(1));

        NotifySubscriberService service = new NotifySubscriberService(new BrokerEventBus(), clusterManager, providers){
            @Override
            public ReactiveRepository<NotifySubscriberEntity, String> getRepository() {
                return repository;
            }
        };
        assertNotNull(service);
        NotifySubscriberEntity notifySubscriberEntity = new NotifySubscriberEntity();
        notifySubscriberEntity.setId("test");
        notifySubscriberEntity.setTopicProvider("device_alarm");
        notifySubscriberEntity.setSubscriberType("test");
        notifySubscriberEntity.setSubscriber("test");
        service.doSubscribe(notifySubscriberEntity)
            .as(StepVerifier::create)
            .expectSubscription()
            .verifyComplete();

        NotifySubscriberEntity notifySubscriberEntity1 = new NotifySubscriberEntity();
        notifySubscriberEntity1.setTopicProvider("device_alarm");
        notifySubscriberEntity1.setSubscriberType("test");
        notifySubscriberEntity1.setSubscriber("test");
        service.doSubscribe(notifySubscriberEntity1)
            .as(StepVerifier::create)
            .expectError()
            .verify();

        notifySubscriberEntity.setTopicProvider("aa");
        service.doSubscribe(notifySubscriberEntity1)
            .as(StepVerifier::create)
            .expectError()
            .verify();

    }


    protected void initAuth(TestAuthentication authentication) {
        authentication.addPermission("test","test");
        authentication.addDimension("test","test");
        authentication.addDimension("org","org");
    }
    @Test
    void run(){
        ClusterManager clusterManager = Mockito.mock(ClusterManager.class);
        ReactiveRepository<NotifySubscriberEntity, String> repository = Mockito.mock(ReactiveRepository.class);
        ReactiveQuery<NotifySubscriberEntity> query = Mockito.mock(ReactiveQuery.class);
        List<SubscriberProvider> providers = new ArrayList<>();
        DeviceAlarmProvider deviceAlarmProvider = new DeviceAlarmProvider(new BrokerEventBus());
        providers.add(deviceAlarmProvider);

        NotifySubscriberEntity notifySubscriberEntity = new NotifySubscriberEntity();
        notifySubscriberEntity.setId("test");
        notifySubscriberEntity.setTopicProvider("device_alarm");
        notifySubscriberEntity.setSubscriberType("test");
        notifySubscriberEntity.setSubscriber("test");
        notifySubscriberEntity.setState(SubscribeState.enabled);

        Mockito.when(repository.createQuery())
            .thenReturn(query);
        Mockito.when(query.where(Mockito.any(StaticMethodReferenceColumn.class),Mockito.any(Object.class)))
            .thenReturn(query);
        Mockito.when(query.fetch())
            .thenReturn(Flux.just(notifySubscriberEntity));
//        RedisClusterTopic redisClusterTopic = new RedisClusterTopic("notification-changed",);
        ClusterTopic<Object> redisClusterTopic = Mockito.mock(ClusterTopic.class);
        Mockito.when(clusterManager.getTopic(Mockito.anyString()))
            .thenReturn(redisClusterTopic);
        Mockito.when(redisClusterTopic.subscribe())
            .thenReturn(Flux.just(notifySubscriberEntity));

        ReactiveAuthenticationHolder.setSupplier(new ReactiveAuthenticationSupplier() {
            @Override
            public Mono<Authentication> get() {
                TestAuthentication authentication = new TestAuthentication("test");
                initAuth(authentication);
                return Mono.just(authentication);
            }

            @Override
            public Mono<Authentication> get(String userId) {
                TestAuthentication authentication = new TestAuthentication(userId);
                initAuth(authentication);
                return Mono.just(authentication);
            }
        });

        EventBus eventBus = Mockito.mock(EventBus.class);
        Mockito.when(eventBus.publish(Mockito.anyString(),Mockito.any(Notification.class)))
            .thenReturn(Mono.just(1L));
        NotifySubscriberService service = new NotifySubscriberService(eventBus, clusterManager, providers){
            @Override
            public ReactiveRepository<NotifySubscriberEntity, String> getRepository() {
                return repository;
            }
        };
        assertNotNull(service);
        service.run();

        notifySubscriberEntity.setState(SubscribeState.disabled);
        service.run();
        Mockito.when(eventBus.publish(Mockito.anyString(),Mockito.any(Notification.class)))
            .thenReturn(Mono.error(()->new NotFoundException()));
        service.run();
    }


}
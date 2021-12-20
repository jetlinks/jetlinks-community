package org.jetlinks.community.auth.service;

import org.hswebframework.ezorm.core.StaticMethodReferenceColumn;
import org.hswebframework.ezorm.rdb.mapping.ReactiveDelete;
import org.hswebframework.ezorm.rdb.mapping.ReactiveQuery;
import org.hswebframework.ezorm.rdb.mapping.ReactiveRepository;
import org.hswebframework.ezorm.rdb.mapping.defaults.SaveResult;
import org.hswebframework.web.authorization.DimensionProvider;
import org.hswebframework.web.authorization.simple.SimpleAuthentication;
import org.hswebframework.web.system.authorization.api.entity.AuthorizationSettingEntity;
import org.hswebframework.web.system.authorization.defaults.configuration.PermissionProperties;
import org.hswebframework.web.system.authorization.defaults.service.DefaultAuthorizationSettingService;
import org.hswebframework.web.system.authorization.defaults.service.DefaultDimensionService;
import org.jetlinks.community.auth.web.request.AuthorizationSettingDetail;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.reactivestreams.Publisher;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.util.ArrayList;
import java.util.List;

class AuthorizationSettingDetailServiceTest {

    @Test
    void saveDetail() {
        DefaultAuthorizationSettingService settingService = Mockito.mock(DefaultAuthorizationSettingService.class);
        ReactiveRepository<AuthorizationSettingEntity, String> repository = Mockito.mock(ReactiveRepository.class);
        ReactiveDelete delete = Mockito.mock(ReactiveDelete.class);

        Mockito.when(settingService.getRepository())
            .thenReturn(repository);
        Mockito.when(repository.createDelete())
            .thenReturn(delete);
        Mockito.when(delete.where(Mockito.any(StaticMethodReferenceColumn.class), Mockito.any(Object.class)))
            .thenReturn(delete);
        Mockito.when(delete.and(Mockito.any(StaticMethodReferenceColumn.class), Mockito.any(Object.class)))
            .thenReturn(delete);
        Mockito.when(delete.execute())
            .thenReturn(Mono.just(1));

        Mockito.when(settingService.save(Mockito.any(Publisher.class)))
            .thenReturn(Mono.just(SaveResult.of(1,0)));

        List<DimensionProvider> providers = new ArrayList<>();
        DefaultDimensionService defaultDimensionService = new DefaultDimensionService();
        providers.add(defaultDimensionService);

        AuthorizationSettingDetailService authorizationSettingDetailService = new AuthorizationSettingDetailService(settingService, providers, new PermissionProperties());

        AuthorizationSettingDetail authorizationSettingDetail = new AuthorizationSettingDetail();
        authorizationSettingDetail.setTargetId("test");
        authorizationSettingDetail.setTargetType("test");
        AuthorizationSettingDetail authorizationSettingDetail1 = new AuthorizationSettingDetail();
        authorizationSettingDetail1.setTargetId("test1");
        authorizationSettingDetail1.setTargetType("test1");
        authorizationSettingDetailService.saveDetail(new SimpleAuthentication(), Flux.just(authorizationSettingDetail,authorizationSettingDetail1))
            .as(StepVerifier::create)
            .expectNext()
            .verifyComplete();

    }

    @Test
    void getSettingDetail() {
        DefaultAuthorizationSettingService settingService = Mockito.mock(DefaultAuthorizationSettingService.class);
        ReactiveQuery<AuthorizationSettingEntity> query = Mockito.mock(ReactiveQuery.class);
        AuthorizationSettingDetailService authorizationSettingDetailService = new AuthorizationSettingDetailService(settingService, new ArrayList<DimensionProvider>(), new PermissionProperties());

        AuthorizationSettingEntity authorizationSettingEntity = new AuthorizationSettingEntity();
        authorizationSettingEntity.setId("test");
        authorizationSettingEntity.setDimensionTarget("test");
        authorizationSettingEntity.setDimensionType("test");
        Mockito.when(settingService.createQuery())
            .thenReturn(query);
        Mockito.when(query.where(Mockito.any(StaticMethodReferenceColumn.class),Mockito.any(Object.class)))
            .thenReturn(query);
        Mockito.when(query.and(Mockito.any(StaticMethodReferenceColumn.class),Mockito.any(Object.class)))
            .thenReturn(query);
        Mockito.when(query.fetch())
            .thenReturn(Flux.just(authorizationSettingEntity));

        authorizationSettingDetailService.getSettingDetail("test","test")
            .map(AuthorizationSettingDetail::getTargetType)
            .as(StepVerifier::create)
            .expectNext("test")
            .verifyComplete();
    }
}
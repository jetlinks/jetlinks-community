package org.jetlinks.community.auth.service;

import org.hswebframework.ezorm.core.StaticMethodReferenceColumn;
import org.hswebframework.ezorm.core.param.QueryParam;
import org.hswebframework.ezorm.rdb.mapping.ReactiveDelete;
import org.hswebframework.ezorm.rdb.mapping.ReactiveQuery;
import org.hswebframework.ezorm.rdb.mapping.ReactiveRepository;
import org.hswebframework.ezorm.rdb.mapping.defaults.SaveResult;
import org.hswebframework.web.crud.events.EntityDeletedEvent;
import org.hswebframework.web.system.authorization.api.entity.DimensionUserEntity;
import org.hswebframework.web.system.authorization.defaults.service.DefaultDimensionUserService;
import org.jetlinks.community.auth.entity.OrganizationEntity;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.reactivestreams.Publisher;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.util.Arrays;
import java.util.List;
import java.util.function.BiFunction;

/**
 * 输入描述.
 *
 * @author zhangji
 * @version 1.11 2021/10/19
 */
public class OrganizationServiceTest {
    public static final String ORG_ID      = "org-test";
    public static final String TENANT_ID_1 = "tenant-1";
    public static final String TENANT_ID_2 = "tenant-2";
    public static final String USER_ID_1   = "user-1";
    public static final String USER_ID_2   = "user-2";
    public static final String USER_ID_3   = "user-3";

    @Test
    void test() {
        // 模拟DefaultDimensionUserService
        ReactiveRepository<DimensionUserEntity, String> dimensionUserrepository = Mockito.mock(ReactiveRepository.class);
        ReactiveDelete reactiveDelete = Mockito.mock(ReactiveDelete.class);
        ReactiveQuery<DimensionUserEntity> dimensionUserQuery = Mockito.mock(ReactiveQuery.class);
        Mockito.when(dimensionUserrepository.save(Mockito.any(Publisher.class)))
            .thenReturn(Mono.just(SaveResult.of(3, 0)));
        Mockito.when(dimensionUserrepository.createDelete())
            .thenReturn(reactiveDelete);
        Mockito.when(reactiveDelete.where(Mockito.any(StaticMethodReferenceColumn.class), Mockito.any()))
            .thenReturn(reactiveDelete);
        Mockito.when(reactiveDelete.in(Mockito.any(StaticMethodReferenceColumn.class), Mockito.anyCollection()))
            .thenReturn(reactiveDelete);
        Mockito.when(reactiveDelete.and(Mockito.any(StaticMethodReferenceColumn.class), Mockito.any()))
            .thenReturn(reactiveDelete);
        Mockito.when(reactiveDelete.execute())
            .thenReturn(Mono.just(1));
        Mockito.when(reactiveDelete.onExecute(Mockito.any(BiFunction.class)))
            .thenReturn(reactiveDelete);
        Mockito.when(dimensionUserrepository.createQuery())
            .thenReturn(dimensionUserQuery);
        Mockito.when(dimensionUserQuery.select(Mockito.any(StaticMethodReferenceColumn.class)))
            .thenReturn(dimensionUserQuery);
        Mockito.when(dimensionUserQuery.setParam(Mockito.any(QueryParam.class)))
            .thenReturn(dimensionUserQuery);
        Mockito.when(dimensionUserQuery.fetch())
            .thenReturn(Flux.fromIterable(dimensionUserEntityList()));

        DefaultDimensionUserService dimensionUserService = new DefaultDimensionUserService() {
            @Override
            public ReactiveRepository<DimensionUserEntity, String> getRepository() {
                return dimensionUserrepository;
            }

            @Override
            public Mono<SaveResult> save(Publisher<DimensionUserEntity> entityPublisher) {
                return Flux.from(entityPublisher)
                    .count()
                    .map(Long::intValue)
                    .map(i -> SaveResult.of(i, 0));
            }
        };


        // 模拟repostory
        ReactiveRepository<OrganizationEntity, String> repository = Mockito.mock(ReactiveRepository.class);
        Mockito.when(repository.findById(Mockito.anyString()))
            .thenReturn(Mono.just(organizationEntity()));

        // 构建OrganizationService
        OrganizationService organizationService = new OrganizationService(dimensionUserService) {
            @Override
            public ReactiveRepository<OrganizationEntity, String> getRepository() {
                return repository;
            }
        };

        // 绑定用户
        organizationService.bindUser(
            ORG_ID, Arrays.asList(USER_ID_1, USER_ID_2, USER_ID_3))
            .as(StepVerifier::create)
            .expectNext(3)
            .verifyComplete();

        // 绑定用户-不指定租户ID
        organizationService.bindUser(ORG_ID, Arrays.asList(USER_ID_1, USER_ID_2, USER_ID_3))
            .as(StepVerifier::create)
            .expectNext(3)
            .verifyComplete();

        // 解绑用户
        organizationService.unbindUser(ORG_ID, Arrays.asList(USER_ID_1))
            .as(StepVerifier::create)
            .expectNext(1)
            .verifyComplete();

        // 解绑用户-不指定租户ID
        organizationService.unbindUser(ORG_ID, Arrays.asList(USER_ID_1))
            .as(StepVerifier::create)
            .expectNext(1)
            .verifyComplete();

        EntityDeletedEvent<OrganizationEntity> event = new EntityDeletedEvent<>(
            Arrays.asList(organizationEntity()), OrganizationEntity.class
        );
        event.getAsync()
            .as(StepVerifier::create)
            .expectComplete();
    }

    private OrganizationEntity organizationEntity() {
        OrganizationEntity organizationEntity = new OrganizationEntity();
        organizationEntity.setId(ORG_ID);
        organizationEntity.setName("测试机构");

        return organizationEntity;
    }

    private List<DimensionUserEntity> dimensionUserEntityList() {
        DimensionUserEntity dimensionUserEntity1 = new DimensionUserEntity();
        dimensionUserEntity1.setId("1");
        dimensionUserEntity1.setDimensionId(TENANT_ID_1);
        dimensionUserEntity1.setUserId("test");

        DimensionUserEntity dimensionUserEntity2 = new DimensionUserEntity();
        dimensionUserEntity2.setId("2");
        dimensionUserEntity2.setDimensionId(TENANT_ID_2);
        dimensionUserEntity2.setUserId("test");

        return Arrays.asList(dimensionUserEntity1, dimensionUserEntity2);
    }

}

package org.jetlinks.community.auth.relation;

import org.hswebframework.ezorm.core.StaticMethodReferenceColumn;
import org.hswebframework.ezorm.rdb.mapping.ReactiveRepository;
import org.hswebframework.ezorm.rdb.mapping.defaults.DefaultReactiveQuery;
import org.jetlinks.community.auth.entity.ThirdPartyUserBindEntity;
import org.jetlinks.community.auth.entity.UserDetail;
import org.jetlinks.community.auth.service.UserDetailService;
import org.jetlinks.community.relation.RelationConstants;
import org.jetlinks.core.things.relation.PropertyOperation;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

class UserRelationObjectProviderTest {

    @Test
    void test() {
        UserDetailService service = Mockito.mock(UserDetailService.class);
        ReactiveRepository<ThirdPartyUserBindEntity, String> repository = Mockito.mock(ReactiveRepository.class);
        UserDetail detail = new UserDetail();
        detail.setName("Admin");
        detail.setUsername("admin");
        detail.setId("admin");
        detail.setEmail("admin@hsweb.me");
        {

            Mockito.when(service.findUserDetail("admin"))
                   .thenReturn(Mono.just(detail));

        }
        ThirdPartyUserBindEntity entity = new ThirdPartyUserBindEntity();
        entity.setType("wx");
        entity.setThirdPartyUserId("third-admin");
        entity.setProvider("dingtalk");
        entity.setUserId("admin");
        {



            DefaultReactiveQuery query = Mockito.mock(DefaultReactiveQuery.class);

            Mockito.when(query.where(Mockito.any(StaticMethodReferenceColumn.class),Mockito.any()))
                       .thenReturn(query);

            Mockito.when(query.and(Mockito.any(StaticMethodReferenceColumn.class),Mockito.any()))
                   .thenReturn(query);

            Mockito.when(query.fetch())
                   .thenReturn(Flux.just(
                       entity
                   ));

            Mockito.when(repository.createQuery())
                   .thenReturn(query);
        }


        UserRelationObjectProvider provider = new UserRelationObjectProvider(service, repository);

        PropertyOperation operation = provider.properties("admin");

        operation.get(RelationConstants.UserProperty.name)
                 .as(StepVerifier::create)
                 .expectNext(detail.getName())
                 .verifyComplete();

        operation.get(RelationConstants.UserProperty.email)
                 .as(StepVerifier::create)
                 .expectNext(detail.getEmail())
                 .verifyComplete();

        operation.get("null")
                 .as(StepVerifier::create)
                 .expectComplete()
                 .verify();

        operation.get(RelationConstants.UserProperty.thirdParty(entity.getType(),entity.getProvider()))
            .as(StepVerifier::create)
            .expectNext(entity.getThirdPartyUserId())
            .verifyComplete();

    }


}
package org.jetlinks.community.auth.service;

import org.hswebframework.ezorm.rdb.mapping.ReactiveRepository;
import org.hswebframework.web.system.authorization.api.entity.UserEntity;
import org.hswebframework.web.system.authorization.api.service.reactive.ReactiveUserService;
import org.jetlinks.community.auth.entity.UserDetail;
import org.jetlinks.community.auth.entity.UserDetailEntity;
import org.jetlinks.community.auth.service.request.SaveUserDetailRequest;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.reactivestreams.Publisher;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class UserDetailServiceTest {

    private static final String USER_ID="abc";

    @Test
    void findUserDetail() {
        ReactiveRepository<UserDetailEntity, String> repository = Mockito.mock(ReactiveRepository.class);
        ReactiveUserService userService = Mockito.mock(ReactiveUserService.class);

        UserEntity userEntity = new UserEntity();
        userEntity.setId(USER_ID);
        userEntity.setName("tom");
        Mockito.when(userService.findById(Mockito.anyString()))
            .thenReturn(Mono.just(userEntity));
        Mockito.when(repository.findById(Mockito.anyString()))
            .thenReturn(Mono.empty());
        UserDetailService userDetailService = new UserDetailService(userService){
            @Override
            public ReactiveRepository<UserDetailEntity, String> getRepository() {
                return repository;
            }
        };
        assertNotNull(userDetailService);
        userDetailService.findUserDetail(USER_ID)
            .map(UserDetail::getName)
            .as(StepVerifier::create)
            .expectNext("tom")
            .verifyComplete();

        UserDetailEntity userDetailEntity = new UserDetailEntity();
        userDetailEntity.setTelephone("18999999999");
        Mockito.when(repository.findById(Mockito.anyString()))
            .thenReturn(Mono.just(userDetailEntity));
        userDetailService.findUserDetail(USER_ID)
            .map(UserDetail::getTelephone)
            .as(StepVerifier::create)
            .expectNext("18999999999")
            .verifyComplete();
    }

    @Test
    void saveUserDetail() {
        ReactiveRepository<UserDetailEntity, String> repository = Mockito.mock(ReactiveRepository.class);
        ReactiveUserService userService = Mockito.mock(ReactiveUserService.class);
        UserEntity userEntity = new UserEntity();
        userEntity.setId(USER_ID);
        Mockito.when(repository.save(Mockito.any(Publisher.class)))
            .thenReturn(Mono.just(userEntity));
        Mockito.when(userService.saveUser(Mockito.any(Mono.class)))
            .thenReturn(Mono.just(true));

        UserDetailService userDetailService = new UserDetailService(userService){
            @Override
            public ReactiveRepository<UserDetailEntity, String> getRepository() {
                return repository;
            }
        };
        assertNotNull(userDetailService);
        SaveUserDetailRequest saveUserDetailRequest = new SaveUserDetailRequest();
        saveUserDetailRequest.setName("tom");
        saveUserDetailRequest.setDescription("sss");
        saveUserDetailRequest.setEmail("1149127931@qq.com");
        saveUserDetailRequest.setAvatar("ccc");
        saveUserDetailRequest.setTelephone("18999999999");
        userDetailService.saveUserDetail(USER_ID,saveUserDetailRequest)
            .as(StepVerifier::create)
            .expectSubscription()
            .verifyComplete();
    }
}
package org.jetlinks.community.rule.engine.service;

import org.hswebframework.web.crud.service.GenericReactiveCrudService;
import org.jetlinks.community.rule.engine.entity.TestEntity;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

@Service
public class TestService extends GenericReactiveCrudService<TestEntity,String> {


    public TestEntity getValue(String id){
        TestEntity testEntity = super
            .getRepository()
            .findById(id)
            .block();
        return testEntity;
    }


}

package org.jetlinks.community.rule.engine.service;

import org.hswebframework.web.crud.service.GenericReactiveCrudService;
import org.jetlinks.community.rule.engine.entity.TestEntity;
import org.springframework.stereotype.Service;

@Service
public class TestService extends GenericReactiveCrudService<TestEntity,String> {


    public TestEntity getValue(String value){
        TestEntity block = super.getRepository().findById(value).block();
        return block;
    }
}

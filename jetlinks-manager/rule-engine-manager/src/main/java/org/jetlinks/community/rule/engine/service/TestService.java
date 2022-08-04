package org.jetlinks.community.rule.engine.service;

import org.hswebframework.web.crud.service.GenericReactiveCrudService;
import org.jetlinks.community.rule.engine.entity.TestEntity;
import org.springframework.stereotype.Service;

@Service
public class TestService extends GenericReactiveCrudService<TestEntity,String> {


    public String getValue(String value){
        String s = super.getRepository().findById(value).toString();
        return s;
    }
}

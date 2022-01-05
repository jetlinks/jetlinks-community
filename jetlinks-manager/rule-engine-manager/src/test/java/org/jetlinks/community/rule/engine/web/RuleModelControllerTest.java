package org.jetlinks.community.rule.engine.web;

import com.alibaba.fastjson.JSON;
import org.hswebframework.web.crud.service.ReactiveCrudService;
import org.jetlinks.community.rule.engine.entity.RuleModelEntity;
import org.jetlinks.community.rule.engine.enums.SqlRuleType;
import org.jetlinks.community.rule.engine.ql.SqlRule;
import org.jetlinks.community.rule.engine.service.RuleModelService;
import org.jetlinks.community.test.spring.TestJetLinksController;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.WebFluxTest;
import reactor.core.publisher.Mono;

import java.util.ArrayList;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

@WebFluxTest(RuleModelController.class)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class RuleModelControllerTest extends TestJetLinksController {
    private static final String BASE_URL="/rule-engine/model";
    private static final String ID="test";

    @Autowired
    private RuleModelService ruleModelService;
    @Test
    @Order(0)
    void save(){
        assertNotNull(ruleModelService);
        RuleModelEntity entity = new RuleModelEntity();
        entity.setId(ID);
        entity.setVersion(1);
        entity.setDescription("test");
        entity.setModelType("sql_rule");
        SqlRule sqlRule = new SqlRule();
        sqlRule.setId("test");
        sqlRule.setCron("test");
        sqlRule.setSql("select * from rule_instance");
        sqlRule.setType(SqlRuleType.realTime);
        sqlRule.setActions(new ArrayList<>());
        sqlRule.setWhenErrorThen(new ArrayList<>());
        String s = JSON.toJSONString(sqlRule);
        entity.setModelMeta(s);
        ruleModelService.save(Mono.just(entity)).subscribe();
    }
    @Test
    void getService() {
        ReactiveCrudService<RuleModelEntity, String> service =
            new RuleModelController().getService();
        assertNull(service);
    }

    @Test
    @Order(1)
    void deploy() {
        assertNotNull(client);
        client.post()
            .uri(BASE_URL+"/"+ID+"/_deploy")
            .exchange()
            .expectStatus()
            .is2xxSuccessful();
    }

    @Test
    void getAllSupportExecutors() {
        assertNotNull(client);
        client.get()
            .uri(BASE_URL+"/executors")
            .exchange()
            .expectStatus()
            .is2xxSuccessful();
    }
}
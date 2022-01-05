package org.jetlinks.community.rule.engine.web;

import com.alibaba.fastjson.JSON;
import org.hswebframework.web.crud.service.ReactiveCrudService;
import org.jetlinks.community.rule.engine.entity.RuleInstanceEntity;
import org.jetlinks.community.rule.engine.enums.RuleInstanceState;
import org.jetlinks.community.rule.engine.enums.SqlRuleType;
import org.jetlinks.community.rule.engine.ql.SqlRule;
import org.jetlinks.community.rule.engine.service.RuleInstanceService;
import org.jetlinks.community.test.spring.TestJetLinksController;
import org.jetlinks.rule.engine.api.RuleData;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.WebFluxTest;

import java.util.ArrayList;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

@WebFluxTest(RuleInstanceController.class)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class RuleInstanceControllerTest extends TestJetLinksController {
    private static final String BASE_URL="/rule-engine/instance";
    private static final String InstanceID="ins";
    @Autowired
    private RuleInstanceService instanceService;
    @Test
    @Order(0)
    void save(){
        assertNotNull(instanceService);
        RuleInstanceEntity entity = new RuleInstanceEntity();
        entity.setId(InstanceID);
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
        entity.setName("test");
        entity.setModelId("test");
        entity.setCreateTime(System.currentTimeMillis());
        entity.setCreatorId("test");
        entity.setDescription("test");
        entity.setInstanceDetailJson("test");
        entity.setModelVersion(1);
        entity.setState(RuleInstanceState.started);
        entity.setCreatorName("test");

        instanceService.save(entity).subscribe();
    }

    @Test
    @Order(1)
    void getTasks() {
        assertNotNull(client);
        client.get()
            .uri(BASE_URL+"/"+InstanceID+"/tasks")
            .exchange()
            .expectStatus()
            .is2xxSuccessful();
    }

    @Test
    @Order(1)
    void getRuleNodeList() {
        assertNotNull(client);
        client.get()
            .uri(BASE_URL+"/"+InstanceID+"/nodes")
            .exchange()
            .expectStatus()
            .is2xxSuccessful();
    }

    @Test
    @Order(1)
    void start() {
        assertNotNull(client);
        client.post()
            .uri(BASE_URL+"/"+InstanceID+"/_start")
            .exchange()
            .expectStatus()
            .is2xxSuccessful();

    }

    @Test
    @Order(2)
    void stop() {
        assertNotNull(client);
        client.post()
            .uri(BASE_URL+"/"+InstanceID+"/_stop")
            .exchange()
            .expectStatus()
            .is2xxSuccessful();
    }

    @Test
    @Order(1)
    void queryLog() {
        assertNotNull(client);
        client.get()
            .uri(BASE_URL+"/"+InstanceID+"/logs")
            .exchange()
            .expectStatus()
            .is2xxSuccessful();
    }

    @Test
    @Order(1)
    void queryEvents() {
        assertNotNull(client);
        client.get()
            .uri(BASE_URL+"/"+InstanceID+"/events")
            .exchange()
            .expectStatus()
            .is2xxSuccessful();
    }

    @Test
    @Order(1)
    void execute() {
//        RuleInstanceEntity entity = new RuleInstanceEntity();
//        entity.setId("test");
//        entity.setModelId("modelId");
//        entity.setName("test");
//        entity.setModelType("ss");
//        entity.setModelMeta("test");
//        RuleModel model = entity.toRule(modelParser);
//        ruleEngine.startRule(entity.getId(),model).subscribe();
        assertNotNull(client);
        RuleData ruleData = new RuleData();
        ruleData.setId("test");
        client.post()
            .uri(BASE_URL+"/local/"+InstanceID+"/_execute")
            .bodyValue(ruleData)
            .exchange()
            .expectStatus()
            .is4xxClientError();

    }

    @Test
    @Order(1)
    void getService() {
        ReactiveCrudService<RuleInstanceEntity, String> service =
            new RuleInstanceController().getService();
        assertNull(service);
    }
}
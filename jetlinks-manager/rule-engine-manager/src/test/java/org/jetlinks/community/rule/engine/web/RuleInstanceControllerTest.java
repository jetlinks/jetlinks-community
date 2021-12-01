package org.jetlinks.community.rule.engine.web;

import com.alibaba.fastjson.JSON;
import org.jetlinks.community.rule.engine.entity.RuleInstanceEntity;
import org.jetlinks.community.rule.engine.enums.RuleInstanceState;
import org.jetlinks.community.rule.engine.enums.SqlRuleType;
import org.jetlinks.community.rule.engine.ql.SqlRule;
import org.jetlinks.community.rule.engine.service.RuleInstanceService;
import org.jetlinks.community.rule.engine.test.spring.TestJetLinksController;
import org.jetlinks.rule.engine.api.RuleData;
import org.jetlinks.rule.engine.api.RuleEngine;
import org.jetlinks.rule.engine.api.model.RuleEngineModelParser;
import org.jetlinks.rule.engine.api.model.RuleModel;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.WebFluxTest;

import java.util.ArrayList;

import static org.junit.jupiter.api.Assertions.*;

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
        client.get()
            .uri(BASE_URL+"/"+InstanceID+"/tasks")
            .exchange()
            .expectStatus()
            .is2xxSuccessful();
    }

    @Test
    @Order(1)
    void getRuleNodeList() {
        client.get()
            .uri(BASE_URL+"/"+InstanceID+"/nodes")
            .exchange()
            .expectStatus()
            .is2xxSuccessful();
    }

    @Test
    @Order(1)
    void start() {

        client.post()
            .uri(BASE_URL+"/"+InstanceID+"/_start")
            .exchange()
            .expectStatus()
            .is2xxSuccessful();

    }

    @Test
    @Order(2)
    void stop() {
        client.post()
            .uri(BASE_URL+"/"+InstanceID+"/_stop")
            .exchange()
            .expectStatus()
            .is2xxSuccessful();
    }

    @Test
    @Order(1)
    void queryLog() {
        client.get()
            .uri(BASE_URL+"/"+InstanceID+"/logs")
            .exchange()
            .expectStatus()
            .is2xxSuccessful();
    }

    @Test
    @Order(1)
    void queryEvents() {
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
        new RuleInstanceController().getService();
    }
}
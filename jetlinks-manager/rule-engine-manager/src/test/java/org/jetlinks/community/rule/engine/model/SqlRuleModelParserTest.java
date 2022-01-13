package org.jetlinks.community.rule.engine.model;

import com.alibaba.fastjson.JSON;
import org.jetlinks.community.rule.engine.enums.SqlRuleType;
import org.jetlinks.community.rule.engine.ql.SqlRule;
import org.jetlinks.rule.engine.api.model.RuleModel;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class SqlRuleModelParserTest {
    
    @Test
    void parse() {
        SqlRuleModelParser parser = new SqlRuleModelParser();
        SqlRule sqlRule = new SqlRule();
        sqlRule.setSql("select * from table");
        sqlRule.setId("id");
        sqlRule.setCron("1 1 1 1 1 1");
        sqlRule.setName("name");
        sqlRule.setType(SqlRuleType.timer);
        List<Action> whenErrorThen = new ArrayList<>();
        Action action = new Action();
        Action action1 = new Action();
        action1.setExecutor("ex");
        action1.setConfiguration(new HashMap<>());
        whenErrorThen.add(action);
        whenErrorThen.add(action1);
        sqlRule.setWhenErrorThen(whenErrorThen);
        List<Action> actions = new ArrayList<>();
        actions.add(action);
        actions.add(action1);
        sqlRule.setActions(actions);
        String s = JSON.toJSONString(sqlRule);

        RuleModel parse = parser.parse(s);
        assertNotNull(parse);
    }
}
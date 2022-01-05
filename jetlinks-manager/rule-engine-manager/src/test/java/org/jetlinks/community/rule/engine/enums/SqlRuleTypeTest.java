package org.jetlinks.community.rule.engine.enums;

import org.jetlinks.community.rule.engine.ql.SqlRule;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;

import static org.junit.jupiter.api.Assertions.assertThrows;

class SqlRuleTypeTest {

    @Test
    void getValue() {
        SqlRule sqlRule = new SqlRule();
//
        sqlRule.setCron("1 1 1 1 1 1");
        SqlRuleType.timer.validate(sqlRule);
        sqlRule.setCron("s");
        Executable executable = ()-> SqlRuleType.timer.validate(sqlRule);
        assertThrows(IllegalArgumentException.class,executable);

        sqlRule.setSql("select * from table group by interval");
        Executable executable1 = ()-> SqlRuleType.realTime.validate(sqlRule);
        assertThrows(IllegalArgumentException.class,executable1);
        sqlRule.setSql("select * from table group by _window('1s')");
        SqlRuleType.realTime.validate(sqlRule);
        sqlRule.setSql("test");
        Executable executable2 =()-> SqlRuleType.realTime.validate(sqlRule);
        assertThrows(IllegalArgumentException.class,executable2);
    }

}
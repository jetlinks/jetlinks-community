package org.jetlinks.community.rule.engine.ql;

import lombok.Getter;
import lombok.Setter;
import org.jetlinks.community.rule.engine.enums.SqlRuleType;
import org.jetlinks.community.rule.engine.model.Action;
import org.springframework.util.Assert;

import java.io.Serializable;
import java.util.List;

/**
 * 使用SQL来处理数据
 *
 * @author zhouhao
 * @since 1.1
 */
@Getter
@Setter
public class SqlRule implements Serializable {

    private static final long serialVersionUID = -6849794470754667710L;

    private String id;

    private String name;

    private SqlRuleType type;

    private String cron;

    private String sql;

    private List<Action> actions;

    private List<Action> whenErrorThen;

    public void validate() {
        Assert.notNull(type, "type不能为空");

        type.validate(this);
    }
}

/*
 * Copyright 2025 JetLinks https://www.jetlinks.cn
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.jetlinks.community.rule.engine.service.terms;

import org.hswebframework.ezorm.core.param.Term;
import org.hswebframework.ezorm.rdb.metadata.RDBColumnMetadata;
import org.hswebframework.ezorm.rdb.operator.builder.fragments.BatchSqlFragments;
import org.hswebframework.ezorm.rdb.operator.builder.fragments.SqlFragments;
import org.hswebframework.ezorm.rdb.operator.builder.fragments.term.AbstractTermFragmentBuilder;
import org.hswebframework.ezorm.rdb.utils.SqlUtils;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 根据告警配置查询规则.
 * <p>
 * 例如：查询告警ID为alarm-id绑定的场景联动
 * <pre>
 *     {
 *             "column":"id",
 *             "termType":"alarm-bind-rule",
 *             "value":"alarm-id"
 *     }
 * </pre>
 *
 * @author zhangji 2022/11/23
 */
@Component
public class AlarmBindRuleTerm extends AbstractTermFragmentBuilder {

    public AlarmBindRuleTerm() {
        super("alarm-bind-rule", "告警绑定的规则");
    }

    @Override
    public SqlFragments createFragments(String columnFullName,
                                        RDBColumnMetadata column,
                                        Term term) {
        boolean not = term.getOptions().contains("not");

        BatchSqlFragments sqlFragments = new BatchSqlFragments(not ? 7 : 6, 1);

        if (not) {
            sqlFragments.add(SqlFragments.NOT);
        }

        sqlFragments
            .addSql("exists(select 1 from ", getTableName("s_alarm_rule_bind", column), " bind_ where bind_.rule_id =", columnFullName);

        List<Object> alarmId = convertList(column, term);
        sqlFragments
            .addSql("and bind_.alarm_id in (")
            .add(SqlUtils.createQuestionMarks(alarmId.size()))
            //  )
            .add(SqlFragments.RIGHT_BRACKET)
            .addParameter(alarmId);

        sqlFragments.add(SqlFragments.RIGHT_BRACKET);

        return sqlFragments;
    }
}

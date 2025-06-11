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
package org.jetlinks.community.auth.service.terms;

import org.hswebframework.ezorm.core.param.Term;
import org.hswebframework.ezorm.rdb.metadata.RDBColumnMetadata;
import org.hswebframework.ezorm.rdb.operator.builder.fragments.BatchSqlFragments;
import org.hswebframework.ezorm.rdb.operator.builder.fragments.SqlFragments;
import org.hswebframework.ezorm.rdb.operator.builder.fragments.term.AbstractTermFragmentBuilder;
import org.hswebframework.ezorm.rdb.utils.SqlUtils;
import org.jetlinks.community.authorize.OrgDimensionType;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 查询组织关联的用户.
 *
 * 只查询组织绑定的用户：
 * <pre>{@code
 *     "terms":[
 *         {
 *             "column":"id$in-org-user$org",
 *             "value":["orgId"]
 *         }
 *     ]
 * }</pre>
 *
 *
 * 查询组织或职位绑定的用户
 * <pre>{@code
 *     "terms":[
 *         {
 *             "column":"id$in-org-user",
 *             "value":["orgId"]
 *         }
 *     ]
 * }</pre>
 *
 * @author zhangji 2025/2/12
 * @since 2.3
 */
@Component
public class OrgUserTermBuilder extends AbstractTermFragmentBuilder {
    public static final String termType = "in-org-user";

    public OrgUserTermBuilder() {
        super(termType, "组织关联的所有用户");
    }

    @Override
    public SqlFragments createFragments(String columnFullName, RDBColumnMetadata column, Term term) {

        List<Object> values = convertList(column, term);

        BatchSqlFragments fragments = new BatchSqlFragments(15, 4);
        List<String> options = term.getOptions();

        if (options.contains("not")) {
            fragments.add(SqlFragments.NOT);
        }

        // 组织绑定条件，options未指定时也查询
        if (options.contains("org")) {
            fragments.addSql("exists(select 1 from",
                             getTableName("s_dimension_user", column),
                             "d where d.user_id =", columnFullName,
                             "and (");
            fragments.addSql("d.dimension_type_id = ?")
                     .addParameter(OrgDimensionType.org.getId());
            if (!options.contains("any")) {
                fragments
                    .addSql("and d.dimension_id in(")
                    .add(SqlUtils.createQuestionMarks(values.size()))
                    .add(SqlFragments.RIGHT_BRACKET)
                    .addParameter(values);
            }
        } else {
            return fragments;
        }

        fragments.add(SqlFragments.RIGHT_BRACKET)
                 .add(SqlFragments.RIGHT_BRACKET);

        return fragments;
    }
}

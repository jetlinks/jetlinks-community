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

import org.apache.commons.collections4.CollectionUtils;
import org.hswebframework.ezorm.core.param.Term;
import org.hswebframework.ezorm.rdb.metadata.RDBColumnMetadata;
import org.hswebframework.ezorm.rdb.operator.builder.fragments.PrepareSqlFragments;
import org.hswebframework.ezorm.rdb.operator.builder.fragments.SqlFragments;
import org.hswebframework.ezorm.rdb.operator.builder.fragments.term.AbstractTermFragmentBuilder;
import org.hswebframework.web.exception.I18nSupportException;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.List;

/**
 * <pre>{@code
 *
 *   where id$user-third$type = provider
 *
 * }</pre>
 */
@Component
public class ThirdPartyUserTermBuilder extends AbstractTermFragmentBuilder {

    public ThirdPartyUserTermBuilder() {
        super("user-third", "第三方用户绑定信息");
    }

    @Override
    public SqlFragments createFragments(String columnFullName, RDBColumnMetadata column, Term term) {
        PrepareSqlFragments sqlFragments = PrepareSqlFragments.of();
        if (term.getOptions().contains("not")) {
            sqlFragments.addSql("not");
        }
        sqlFragments
            .addSql("exists(select 1 from ",getTableName("s_third_party_user_bind",column)," bind_ where bind_.user_id =", columnFullName);

        if (CollectionUtils.isEmpty(term.getOptions())) {
            throw new I18nSupportException("error.query_conditions_are_not_specified", column.getName()+"$user-third$type");
        }
        String type = term.getOptions().get(0);

        List<Object> idList = convertList(column, term);
        String[] args = new String[idList.size()];
        Arrays.fill(args, "?");
        sqlFragments
            .addSql(
                "and bind_.type = ? and bind_.provider in (", String.join(",", args), ")")
            .addParameter(type)
            .addParameter(idList);

        sqlFragments.addSql(")");

        return sqlFragments;
    }
}

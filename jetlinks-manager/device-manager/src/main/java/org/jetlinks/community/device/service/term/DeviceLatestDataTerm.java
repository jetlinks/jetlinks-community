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
package org.jetlinks.community.device.service.term;

import lombok.extern.slf4j.Slf4j;
import org.hswebframework.ezorm.core.param.Term;
import org.hswebframework.ezorm.rdb.metadata.RDBColumnMetadata;
import org.hswebframework.ezorm.rdb.metadata.RDBTableMetadata;
import org.hswebframework.ezorm.rdb.operator.builder.fragments.*;
import org.hswebframework.ezorm.rdb.operator.builder.fragments.term.AbstractTermFragmentBuilder;
import org.hswebframework.web.api.crud.entity.TermExpressionParser;
import org.jetlinks.community.device.service.data.DatabaseDeviceLatestDataService;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 根据设备最新的数据来查询设备或者与设备关联的数据,如: 查询温度大于30的设备列表.
 * <p>
 * <b>
 * 注意: 查询时指定列名是和设备ID关联的列或者实体类属性名.
 * 如: 查询设备列表时则使用id.
 * 此条件仅支持关系型数据库中的查询.
 * 要使用此功能,必须开启<code>jetlinks.device.storage.latest.enabled=true</code>
 * </b>
 * <p>
 * 在通用查询接口中可以使用动态查询参数中的<code>term.termType</code>来使用此功能.
 * <a href="https://doc.jetlinks.cn/interface-guide/query-param.html">查看动态查询参数说明</a>
 * <p>
 * 在内部通用条件中,可以使用DSL方式创建条件,例如:
 * <pre>
 *     createQuery()
 *     .where()
 *     .and("id","dev-latest$productId","temp > 10") //productId为具体的产品ID
 *     .fetch()
 * </pre>
 *
 * @author zhouhao
 * @since 1.6
 */
@Slf4j
@Component
public class DeviceLatestDataTerm extends AbstractTermFragmentBuilder {
    public DeviceLatestDataTerm() {
        super("dev-latest", "按设备最新属性查询");
    }

    @Override
    public SqlFragments createFragments(String columnFullName, RDBColumnMetadata column, Term term) {
        PrepareSqlFragments sqlFragments = PrepareSqlFragments.of();

        List<String> options = term.getOptions();
        if (options.isEmpty()) {
            log.warn("无法根据设备数据查询,请指定options,如: deviceId$dev-latest$demo-device");
            return sqlFragments.addSql("1=2");
        }
        String tableName = DatabaseDeviceLatestDataService.getLatestTableTableName(options.get(0));

        RDBTableMetadata metadata = column.getOwner().getSchema().getTable(tableName,false).orElse(null);

        if (metadata == null) {
            log.warn("无法根据设备数据查询,不存在的表:{}", tableName);
            return sqlFragments.addSql("1=2");
        }

        String value = String.valueOf(term.getValue());

        List<Term> terms = TermExpressionParser.parse(value);

        SqlFragments fragments = builder.createTermFragments(metadata, terms);

        sqlFragments.addSql("exists(select 1 from ", metadata.getQuoteName(), "_tmp where _tmp.id =", columnFullName);

        if (fragments.isNotEmpty()) {
            sqlFragments.addSql("and")
                .addSql(fragments.getSql())
                .addParameter(fragments.getParameters());
        }
        sqlFragments.addSql(")");

        return sqlFragments;
    }

    static WhereBuilder builder = new WhereBuilder();

    static class WhereBuilder extends AbstractTermsFragmentBuilder<RDBTableMetadata> {

        @Override
        protected SqlFragments createTermFragments(RDBTableMetadata parameter, Term term) {
            RDBColumnMetadata column = parameter.getColumn(term.getColumn()).orElse(null);
            if (column == null) {
                return EmptySqlFragments.INSTANCE;
            }
            return parameter
                .findFeatureNow(TermFragmentBuilder.createFeatureId(term.getTermType()))
                .createFragments(column.getFullName("_tmp"), column, term);
        }

        @Override
        protected SqlFragments createTermFragments(RDBTableMetadata parameter, List<Term> terms) {
            return super.createTermFragments(parameter, terms);
        }
    }


}

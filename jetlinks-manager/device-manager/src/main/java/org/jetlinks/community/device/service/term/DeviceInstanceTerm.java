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

import org.hswebframework.ezorm.core.param.Term;
import org.hswebframework.ezorm.rdb.metadata.RDBColumnMetadata;
import org.hswebframework.ezorm.rdb.metadata.RDBTableMetadata;
import org.hswebframework.ezorm.rdb.metadata.TableOrViewMetadata;
import org.hswebframework.ezorm.rdb.operator.builder.fragments.*;
import org.hswebframework.ezorm.rdb.operator.builder.fragments.term.AbstractTermFragmentBuilder;
import org.jetlinks.community.utils.ConverterUtils;
import org.springframework.stereotype.Component;

import java.util.List;


/**
 * 根据设备查询.
 * <p>
 * 将设备信息的条件嵌套到此条件中
 * <p>
 * <pre>
 * "terms": [
 *      {
 *          "column": "device_id$dev-instance",
 *          "value": [
 *              {
 *                  "column": "product_id",
 *                  "termType": "eq",
 *                  "value": "1"
 *              }
 *          ]
 *      }
 * ],
 * </pre>
 *
 * @author zhouhao
 * @since 1.6
 */
@Component
public class DeviceInstanceTerm extends AbstractTermFragmentBuilder {

    public static final String termType = "dev-instance";

    public DeviceInstanceTerm() {
        super(termType, "根据设备信息查询");
    }

    @Override
    public SqlFragments createFragments(String columnFullName,
                                        RDBColumnMetadata column,
                                        Term term) {
        List<Term> terms = ConverterUtils.convertTerms(term.getValue());
        PrepareSqlFragments sqlFragments = PrepareSqlFragments.of();
        if (term.getOptions().contains("not")) {
            sqlFragments.addSql("not");
        }
        if (term.getOptions().contains("productId")) {
            // 根据产品ID关联
            sqlFragments
                .addSql("exists(select 1 from ", getTableName("dev_device_instance", column), " _dev where _dev.product_id =", columnFullName);
        } else {
            // 根据设备ID关联
            sqlFragments
                .addSql("exists(select 1 from ", getTableName("dev_device_instance", column), " _dev where _dev.id =", columnFullName);
        }

        RDBTableMetadata metadata = column
            .getOwner()
            .getSchema()
            .getTable("dev_device_instance")
            .orElseThrow(() -> new UnsupportedOperationException("unsupported dev_device_instance"));

        SqlFragments where = builder.createTermFragments(metadata, terms);
        if (!where.isEmpty()) {
            sqlFragments.addSql("and")
                .addFragments(where);
        }
        sqlFragments.addSql(")");
        return sqlFragments;
    }


    static DeviceTermsBuilder builder = new DeviceTermsBuilder();

    static class DeviceTermsBuilder extends AbstractTermsFragmentBuilder<TableOrViewMetadata> {

        @Override
        protected SqlFragments createTermFragments(TableOrViewMetadata parameter,
                                                   List<Term> terms) {
            return super.createTermFragments(parameter, terms);
        }

        @Override
        protected SqlFragments createTermFragments(TableOrViewMetadata table,
                                                   Term term) {
            if (term.getValue() instanceof NativeSql) {
                NativeSql sql = ((NativeSql) term.getValue());
                return PrepareSqlFragments.of(sql.getSql(), sql.getParameters());
            }
            return table
                .getColumn(term.getColumn())
                .flatMap(column -> table
                    .findFeature(TermFragmentBuilder.createFeatureId(term.getTermType()))
                    .map(termFragment -> termFragment.createFragments(column.getFullName("_dev"), column, term)))
                .orElse(EmptySqlFragments.INSTANCE);
        }
    }
}

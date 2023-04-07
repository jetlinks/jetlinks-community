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
 * 根据设备产品信息查询设备数据
 *
 * @author bestfeng
 * @since 2.0
 */
@Component
public class DeviceProductInfoTermBuilder extends AbstractTermFragmentBuilder {

    public static final String termType = "product-info";

    public DeviceProductInfoTermBuilder() {
        super(termType, "根据产品信息查询设备数据");
    }


    @SuppressWarnings("all")
    public static List<Term> convertTerms(Object value) {
        return ConverterUtils.convertTerms(value);
    }


    @Override
    public SqlFragments createFragments(String columnFullName, RDBColumnMetadata column, Term term) {
        List<Term> terms = convertTerms(term.getValue());
        PrepareSqlFragments sqlFragments = PrepareSqlFragments.of();
        if(term.getOptions().contains("not")){
            sqlFragments.addSql("not");
        }
        sqlFragments
            .addSql("exists(select 1 from ",getTableName("dev_product",column)," _product where _product.id = ", columnFullName);

        RDBTableMetadata metadata = column
            .getOwner()
            .getSchema()
            .getTable("dev_product")
            .orElseThrow(() -> new UnsupportedOperationException("unsupported dev_product"));

        SqlFragments where = builder.createTermFragments(metadata, terms);
        if (!where.isEmpty()) {
            sqlFragments.addSql("and")
                     .addFragments(where);
        }
        sqlFragments.addSql(")");
        return sqlFragments;
    }


    static ProductTermBuilder builder = new ProductTermBuilder();

    static class ProductTermBuilder extends AbstractTermsFragmentBuilder<TableOrViewMetadata> {

        @Override
        protected SqlFragments createTermFragments(TableOrViewMetadata parameter, List<Term> terms) {
            return super.createTermFragments(parameter, terms);
        }

        @Override
        protected SqlFragments createTermFragments(TableOrViewMetadata table, Term term) {
            if (term.getValue() instanceof NativeSql) {
                NativeSql sql = ((NativeSql) term.getValue());
                return PrepareSqlFragments.of(sql.getSql(), sql.getParameters());
            }
            return table
                .getColumn(term.getColumn())
                .flatMap(column -> table
                    .findFeature(TermFragmentBuilder.createFeatureId(term.getTermType()))
                    .map(termFragment -> termFragment.createFragments(column.getFullName("_product"), column, term)))
                .orElse(EmptySqlFragments.INSTANCE);
        }
    }
}

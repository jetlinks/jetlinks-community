package org.jetlinks.community.device.service.term;

import org.hswebframework.ezorm.core.param.Term;
import org.hswebframework.ezorm.rdb.metadata.RDBColumnMetadata;
import org.hswebframework.ezorm.rdb.operator.builder.fragments.PrepareSqlFragments;
import org.hswebframework.ezorm.rdb.operator.builder.fragments.SqlFragments;
import org.hswebframework.ezorm.rdb.operator.builder.fragments.term.AbstractTermFragmentBuilder;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;


@Component
public class DeviceTypeTerm extends AbstractTermFragmentBuilder {
    public DeviceTypeTerm() {
        super("dev-device-type", "按设备类型查询设备");
    }

    @Override
    public SqlFragments createFragments(String columnFullName, RDBColumnMetadata column, Term term) {
        PrepareSqlFragments sqlFragments = PrepareSqlFragments.of();
        List<Object> idList = convertList(column, term);
        if (term.getOptions().contains("not")) {
            sqlFragments.addSql("not");
        }
        sqlFragments
            .addSql("exists(select 1 from ",getTableName("dev_product",column)," _product where _product.id = " + columnFullName);
        sqlFragments
            .addSql(" and _product.device_type in(");
        sqlFragments.addSql(idList.stream().map(str -> "?").collect(Collectors.joining(",")))
                    .addParameter(idList)
                    .addSql("))");

        return sqlFragments;
    }
}

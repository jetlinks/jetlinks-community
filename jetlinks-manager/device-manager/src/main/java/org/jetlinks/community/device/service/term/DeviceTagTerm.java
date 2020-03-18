package org.jetlinks.community.device.service.term;

import org.apache.commons.collections.CollectionUtils;
import org.hswebframework.ezorm.core.param.Term;
import org.hswebframework.ezorm.rdb.metadata.RDBColumnMetadata;
import org.hswebframework.ezorm.rdb.operator.builder.fragments.EmptySqlFragments;
import org.hswebframework.ezorm.rdb.operator.builder.fragments.PrepareSqlFragments;
import org.hswebframework.ezorm.rdb.operator.builder.fragments.SqlFragments;
import org.hswebframework.ezorm.rdb.operator.builder.fragments.term.AbstractTermFragmentBuilder;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

/**
 * where("id$dev-tag$location","重庆")
 */
@Component
public class DeviceTagTerm extends AbstractTermFragmentBuilder {
    public DeviceTagTerm() {
        super("dev-tag", "根据设备标签查询设备");
    }

    @Override
    public SqlFragments createFragments(String columnFullName, RDBColumnMetadata column, Term term) {
        List<Object> values = convertList(column, term);
        if (values.isEmpty()) {
            return EmptySqlFragments.INSTANCE;
        }

        PrepareSqlFragments fragments = PrepareSqlFragments.of();

        List<String> opts = term.getOptions();

        fragments.addSql("exists(select 1 from dev_device_tags d where d.device_id = ", columnFullName);

        if (CollectionUtils.isNotEmpty(opts)) {
            fragments.addSql("and d.key=?").addParameter(opts.get(0));
        }
        if (values.size() == 1) {
            fragments.addSql("and d.value like ?)")
                .addParameter(values);
        } else {
            fragments.addSql("and d.value in(",
                values.stream().map(r -> "?").collect(Collectors.joining(",")), "))")
                .addParameter(values);
        }

        return fragments;
    }
}

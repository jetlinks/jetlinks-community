package org.jetlinks.community.device.service.term;

import org.hswebframework.ezorm.core.param.Term;
import org.hswebframework.ezorm.rdb.metadata.RDBColumnMetadata;
import org.hswebframework.ezorm.rdb.operator.builder.fragments.PrepareSqlFragments;
import org.hswebframework.ezorm.rdb.operator.builder.fragments.SqlFragments;
import org.hswebframework.ezorm.rdb.operator.builder.fragments.term.AbstractTermFragmentBuilder;
import org.springframework.stereotype.Component;

/**
 * where("id$dev-tag$location","重庆")
 */
@Component
public class DeviceTagTerm extends AbstractTermFragmentBuilder {
    public DeviceTagTerm() {
        super("dev-tag", "根据设备标签查询设备");
    }


    private void acceptTerm(PrepareSqlFragments fragments, String terms) {
        //json
        if (terms.startsWith("[")) {
            //
        }
        String[] tags = terms.split("[,]");
        int len = 0;
        fragments.addSql("and (");

        for (String tag : tags) {
            String[] kv = tag.split("[:]");
            if (kv.length != 2) {
                continue;
            }
            if (len++ > 0) {
                fragments.addSql("or");
            }
            fragments.addSql("(d.key = ? and d.value like ?)").addParameter(kv[0], kv[1]);
        }
        fragments.addSql(")");

    }

    @Override
    public SqlFragments createFragments(String columnFullName, RDBColumnMetadata column, Term term) {
        String val = String.valueOf(term.getValue());

        PrepareSqlFragments fragments = PrepareSqlFragments.of();

        fragments.addSql("exists(select 1 from dev_device_tags d where d.device_id =", columnFullName);

        acceptTerm(fragments,val);

        fragments.addSql(")");

        return fragments;
    }
}

package org.jetlinks.community.device.service.term;

import com.alibaba.fastjson.JSON;
import org.hswebframework.ezorm.core.param.Term;
import org.hswebframework.ezorm.rdb.metadata.RDBColumnMetadata;
import org.hswebframework.ezorm.rdb.operator.builder.fragments.PrepareSqlFragments;
import org.hswebframework.ezorm.rdb.operator.builder.fragments.SqlFragments;
import org.hswebframework.ezorm.rdb.operator.builder.fragments.term.AbstractTermFragmentBuilder;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * where("id$dev-tag$location","重庆")
 */
@Component
public class DeviceTagTerm extends AbstractTermFragmentBuilder {
    public DeviceTagTerm() {
        super("dev-tag", "根据设备标签查询设备");
    }

    private void acceptTerm(PrepareSqlFragments fragments, String terms) {
        List<Map<String, String>> tags;

        //json
        if (terms.startsWith("[")) {
            tags = (List) JSON.parseArray(terms, Map.class);
        } else if (terms.startsWith("{")) {
            acceptTerm(fragments, JSON.parseObject(terms));
            return;
        } else {
            tags = Stream.of(terms.split("[,]"))
                .map(str -> str.split("[:]"))
                .map(str -> {
                    Map<String, String> tag = new HashMap<>();
                    tag.put("key", str[0]);
                    tag.put("value", str.length > 1 ? str[1] : null);
                    return tag;
                })
                .collect(Collectors.toList());
        }
        acceptTerm(fragments, tags);
    }

    private void acceptTerm(PrepareSqlFragments fragments, Map<?, ?> terms) {
        acceptTerm(fragments, terms.entrySet().stream().map(e -> {
            Map<String, String> tag = new HashMap<>();
            tag.put("key", String.valueOf(e.getKey()));
            tag.put("value", String.valueOf(e.getValue()));
            return tag;
        }).collect(Collectors.toList()));
    }

    private void acceptTerm(PrepareSqlFragments fragments, List<Map<String, String>> tags) {

        int len = 0;
        fragments.addSql("and (");
        for (Map<String, String> tag : tags) {
            if (len++ > 0) {
                fragments.addSql("or");
            }
            fragments.addSql("(d.key = ? and d.value like ?)").addParameter(tag.get("key"), tag.get("value"));
        }
        if (tags.isEmpty()) {
            fragments.addSql("1=2");
        }
        fragments.addSql(")");
    }

    @Override
    public SqlFragments createFragments(String columnFullName, RDBColumnMetadata column, Term term) {


        PrepareSqlFragments fragments = PrepareSqlFragments.of();

        fragments.addSql("exists(select 1 from dev_device_tags d where d.device_id =", columnFullName);
        Object value = term.getValue();

        if (value instanceof Map) {
            acceptTerm(fragments, (Map<?, ?>) value);
        } else if (value instanceof List) {
            acceptTerm(fragments, (List<Map<String, String>>) value);
        } else {
            acceptTerm(fragments, String.valueOf(value));
        }

        fragments.addSql(")");

        return fragments;
    }
}

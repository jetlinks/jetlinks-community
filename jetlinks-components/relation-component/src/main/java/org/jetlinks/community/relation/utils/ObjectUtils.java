package org.jetlinks.community.relation.utils;

import com.google.common.collect.Sets;
import org.apache.commons.collections4.CollectionUtils;
import org.hswebframework.ezorm.core.param.Term;
import org.hswebframework.web.api.crud.entity.QueryParamEntity;
import org.jetlinks.core.config.ConfigKey;
import org.jetlinks.core.things.relation.ObjectType;

import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * @author gyl
 * @since 2.2
 */
public class ObjectUtils {
    public static ConfigKey<String> queryPermissionId = ConfigKey.of("queryPermissionId", "查询权限id（为空则不需要验证权限）", String.class);

    //多column同时查询
    public static final String MULTI_COLUMN = "@multi";

    private static final Set<String> DEFAULT_MULTI_COLUMN_PROPERTY = Sets.newHashSet("id", "name");

    public static QueryParamEntity refactorParam(QueryParamEntity param) {
        return refactorParam(param, DEFAULT_MULTI_COLUMN_PROPERTY);
    }

    public static QueryParamEntity refactorParam(QueryParamEntity param, Set<String> columns) {
        List<Term> terms = param.getTerms();
        refactorTerm(columns, terms);
        return param;
    }

    private static void refactorTerm(Set<String> columns, List<Term> terms) {
        if (CollectionUtils.isNotEmpty(terms)) {
            for (Term term : terms) {
                refactorTerm(columns, term.getTerms());
                if (MULTI_COLUMN.equals(term.getColumn())) {
                    columns.forEach(column -> {
                        Term term1 = new Term();
                        term1.setColumn(column);
                        term1.setTermType(term.getTermType());
                        term1.setValue(term.getValue());
                        term.addTerm(term1);
                    });
                    //清空@multi
                    term.setColumn(null);
                    term.setType(null);
                    term.setValue(null);
                }
            }
        }
    }

    //获取查询权限id
    public static Optional<String> getPermissionId(ObjectType object) {
        return object.getExpand(queryPermissionId);
    }

}

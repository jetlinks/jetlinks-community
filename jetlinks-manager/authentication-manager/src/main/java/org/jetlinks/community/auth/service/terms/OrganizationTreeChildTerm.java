package org.jetlinks.community.auth.service.terms;

import org.hswebframework.web.crud.sql.terms.TreeChildTermBuilder;
import org.springframework.stereotype.Component;

/**
 * 查询组织及下级的数据.
 */
@Component
public class OrganizationTreeChildTerm extends TreeChildTermBuilder {

    public static final String TERM_TYPE = "org-child";

    public OrganizationTreeChildTerm() {
        super(TERM_TYPE, "查询组织及下级的数据");
    }

    @Override
    protected String tableName() {
        return "s_organization";
    }
}


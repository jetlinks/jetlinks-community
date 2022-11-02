package org.jetlinks.community.auth.service.terms;

import org.hswebframework.ezorm.core.param.Term;
import org.hswebframework.ezorm.rdb.executor.SqlRequest;
import org.hswebframework.ezorm.rdb.metadata.RDBColumnMetadata;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

class ThirdPartyUserTermBuilderTest {


    @Test
    void test() {
        ThirdPartyUserTermBuilder builder = new ThirdPartyUserTermBuilder();

        RDBColumnMetadata column = new RDBColumnMetadata();
        column.setName("id");
        Term term = new Term();
        term.setColumn("id");
        term.getOptions().add("wx");
        term.setValue("providerId");

        SqlRequest request = builder
            .createFragments("id", column, term)
            .toRequest();
        System.out.printf(request.toNativeSql());
        assertEquals(
            "exists(select 1 from s_third_party_user_bind _bind where _bind.user_id = id and _bind.type = 'wx' and _bind.provider in ( 'providerId' ) )",
            request.toNativeSql());
        assertArrayEquals(request.getParameters(), new Object[]{"wx", "providerId"});
    }
}
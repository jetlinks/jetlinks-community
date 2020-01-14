package org.jetlinks.community.elastic.search;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.hswebframework.ezorm.core.dsl.Query;
import org.hswebframework.ezorm.core.param.QueryParam;
import org.jetlinks.community.elastic.search.index.mapping.IndexMappingMetadata;
import org.jetlinks.community.elastic.search.parser.DefaultLinkTypeParser;
import org.jetlinks.community.elastic.search.parser.DefaultQueryParamTranslateService;
import org.jetlinks.community.elastic.search.parser.QueryParamTranslateService;
import org.junit.Assert;
import org.junit.jupiter.api.Test;

/**
 * @author bsetfeng
 * @since 1.0
 **/
public class ElasticSearchQueryParamTranslatorTest {

    @Test
    public void test() {

        Query query = Query.of(new QueryParam())
                .where()
                .is("methodName", "error")
                .or()
                .in("level", "ERROR", "DEBUG")
                .orNest()
                .or()
                .is("threadId", "44")
                .or()
                .is("lineNumber", "319")
                .between("aaa", "2019-12-11 22:00:10", "2019-12-11 23:00:10")
                .end();

        QueryParamTranslateService translateService = new DefaultQueryParamTranslateService(new DefaultLinkTypeParser());
        SearchSourceBuilder searchSourceBuilder = translateService.translate(query.getParam(), IndexMappingMetadata.getInstance(""));

        JSONObject jsonObject = JSON.parseObject(searchSourceBuilder.query().toString());

        JSONObject boolJson = jsonObject.getJSONObject("bool");
        Assert.assertEquals(boolJson.getJSONArray("must").size(), 1);
        Assert.assertEquals(boolJson.getJSONArray("should").size(), 2);
        System.out.println(searchSourceBuilder.query().toString());
    }
}

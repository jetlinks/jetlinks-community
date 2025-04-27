package org.jetlinks.community.elastic.search.index;

import co.elastic.clients.elasticsearch.indices.IndexSettings;
import co.elastic.clients.json.JsonData;
import com.google.common.collect.Maps;
import lombok.*;
import org.apache.commons.collections4.MapUtils;
import org.jetlinks.community.elastic.search.ElasticSearchSupport;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.HashMap;
import java.util.Map;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@ConfigurationProperties(prefix = "elasticsearch.index.settings")
@Generated
public class ElasticSearchIndexProperties {

    //索引分片数据,通常为es的集群节点数量
    private int numberOfShards = 1;

    //副本数量
    private int numberOfReplicas = 0;

    //字段数量限制
    private long totalFieldsLimit = 2000;

    //默认字符串超过512将不会被索引,无法进行搜索
    private int keywordIgnoreAbove = 512;

    //其他的配置信息,在创建索引时将会设置到settings中
    private Map<String, String> options;

    //是否使用别名进行搜索
    //设置为true将使用别名进行搜索,可通过手动绑定和接触别名来灵活配置搜索到的数据范围.
    //设置为false时,将使用*进行搜索.在一些特殊请求,如索引名前缀类似时可能搜索到错误的数据.
    private boolean useAliasSearch = true;


    public IndexSettings.Builder toSettings(IndexSettings.Builder builder) {

        return ElasticSearchSupport
            .current()
            .applyIndexSettings(this, builder);

    }

    public void addSetting(String key, String value) {
        if (null == options) {
            options = new HashMap<>();
        }
        options.put(key, value);
    }
}

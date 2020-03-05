package org.jetlinks.community.elastic.search.index;

import reactor.core.publisher.Mono;

/**
 * es 索引策略
 *
 * @author zhouhao
 * @version 1.0
 */
public interface ElasticSearchIndexStrategy {

    /**
     * 策略标识
     *
     * @return ID
     */
    String getId();

    /**
     * 获取用于获取保存数据的索引
     *
     * @param index 原始索引名
     * @return 索引名
     */
    String getIndexForSave(String index);

    /**
     * 获取用于搜索的索引
     *
     * @param index 原始索引名
     * @return 索引名
     */
    String getIndexForSearch(String index);

    /**
     * 更新索引
     *
     * @param metadata 索引元数据
     * @return 更新结果
     */
    Mono<Void> putIndex(ElasticSearchIndexMetadata metadata);

    Mono<ElasticSearchIndexMetadata> loadIndexMetadata(String index);
}

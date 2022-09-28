package org.jetlinks.community.reference;

import org.jetlinks.community.strategy.Strategy;
import reactor.core.publisher.Flux;

/**
 * 数据引用提供商,用于提供获取对应类型的引用信息
 *
 * @author zhouhao
 * @since 2.0
 */
public interface DataReferenceProvider extends Strategy {

    /**
     * 引用的数据类型
     *
     * @see DataReferenceManager
     */
    @Override
    String getId();

    /**
     * 获取数据引用信息
     *
     * @param dataId 数据ID
     * @return 引用信息
     */
    Flux<DataReferenceInfo> getReference(String dataId);

    /**
     * 获取全部数据引用信息
     *
     * @return 引用信息
     */
    Flux<DataReferenceInfo> getReferences();
}

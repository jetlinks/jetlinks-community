package org.jetlinks.community.elastic.search.manager;

import org.jetlinks.community.elastic.search.manager.entity.IndexStrategy;

import java.util.Map;

/**
 * @author bsetfeng
 * @since 1.0
 **/
public interface IndexStrategyProvider {

    /**
     * index 时间格式。 ps: yyyy-MM-dd
     *
     * @return
     */
    String getFormat();

    /**
     * index与策略串的连接符
     *
     * @return
     */
    default String connector() {
        return "-";
    }

    /**
     * @see StandardsIndexManagerCenter
     *
     * @return
     */
    Map<String, IndexStrategy> getIndexStrategies();


}

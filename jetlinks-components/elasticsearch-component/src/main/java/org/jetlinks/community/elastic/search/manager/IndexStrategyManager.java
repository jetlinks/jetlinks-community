package org.jetlinks.community.elastic.search.manager;

/**
 * @author bsetfeng
 * @since 1.0
 **/
public interface IndexStrategyManager {

    String getName();

    String getStandardsIndex(String index, String pattern, String connector);
}

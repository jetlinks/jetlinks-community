package org.jetlinks.community.elastic.search.manager;

/**
 * @author bsetfeng
 * @since 1.0
 **/
public interface IndexManager {

    IndexPatternManager getIndexPatternManager();

    IndexStrategyManager getIndexStrategyManager();

    String getFormat();

    String getConnector();

    default String getStandardIndex(String index) {
        return getIndexStrategyManager().getStandardsIndex(index, getIndexPatternManager().getPattern(getFormat()), getConnector());
    }
}
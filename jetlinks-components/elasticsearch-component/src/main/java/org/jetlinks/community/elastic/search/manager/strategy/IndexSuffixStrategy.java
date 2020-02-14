package org.jetlinks.community.elastic.search.manager.strategy;

import org.jetlinks.community.elastic.search.enums.IndexStrategyEnum;
import org.jetlinks.community.elastic.search.manager.IndexStrategyManager;
import org.springframework.stereotype.Component;

/**
 * @author bsetfeng
 * @since 1.0
 **/
@Component
public class IndexSuffixStrategy implements IndexStrategyManager {


    @Override
    public String getName() {
        return IndexStrategyEnum.SUFFIX.getValue();
    }

    @Override
    public String getStandardsIndex(String index, String pattern, String connector) {
        return index.concat(connector).concat(pattern);
    }
}

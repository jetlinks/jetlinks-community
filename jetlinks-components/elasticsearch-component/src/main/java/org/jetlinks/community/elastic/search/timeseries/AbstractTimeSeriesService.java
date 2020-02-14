package org.jetlinks.community.elastic.search.timeseries;

import org.hswebframework.ezorm.core.param.QueryParam;
import org.jetlinks.community.timeseries.TimeSeriesService;

/**
 * @author bsetfeng
 * @since 1.0
 **/
public abstract class AbstractTimeSeriesService implements TimeSeriesService {


    protected QueryParam filterAddDefaultSort(QueryParam queryParam) {

        if (queryParam.getSorts().isEmpty()) {
            queryParam.orderBy("timestamp").desc();
        }
        return queryParam;
    }
}

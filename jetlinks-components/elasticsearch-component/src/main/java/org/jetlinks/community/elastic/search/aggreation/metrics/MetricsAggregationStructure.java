package org.jetlinks.community.elastic.search.aggreation.metrics;

import lombok.*;
import org.hswebframework.utils.StringUtils;
import org.jetlinks.community.elastic.search.aggreation.enums.MetricsType;

/**
 * @author bsetfeng
 * @since 1.0
 **/
@Setter
@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class MetricsAggregationStructure {

    @NonNull
    private String field;

    private String name;

    @NonNull
    private MetricsType type = MetricsType.COUNT;

    /**
     * 缺失值
     */
    private Object missingValue;

    public String getName() {
        if (StringUtils.isNullOrEmpty(name)) {
            name = type.name().concat("_").concat(field);
        }
        return name;
    }
}

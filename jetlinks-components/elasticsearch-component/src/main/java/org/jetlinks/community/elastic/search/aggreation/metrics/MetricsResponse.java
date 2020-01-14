package org.jetlinks.community.elastic.search.aggreation.metrics;

import lombok.*;
import org.jetlinks.community.elastic.search.aggreation.enums.MetricsType;

import java.util.Map;

/**
 * @author bsetfeng
 * @since 1.0
 **/
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class MetricsResponse {

    private Map<MetricsType, MetricsResponseSingleValue> results;

    private MetricsResponseSingleValue singleResult;

    public MetricsResponseSingleValue getSingleResult() {
        if (singleResult == null) {
            this.singleResult = results.entrySet()
                    .stream()
                    .findFirst()
                    .map(Map.Entry::getValue)
                    .orElse(MetricsResponseSingleValue.empty());
        }
        return singleResult;
    }
}

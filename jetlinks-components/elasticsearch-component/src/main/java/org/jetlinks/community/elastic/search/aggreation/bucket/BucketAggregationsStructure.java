package org.jetlinks.community.elastic.search.aggreation.bucket;

import lombok.*;
import org.elasticsearch.search.aggregations.bucket.histogram.LongBounds;
import org.hswebframework.utils.StringUtils;
import org.jetlinks.community.elastic.search.aggreation.enums.BucketType;
import org.jetlinks.community.elastic.search.aggreation.metrics.MetricsAggregationStructure;

import java.util.LinkedList;
import java.util.List;

/**
 * @author bsetfeng
 * @since 1.0
 **/
@Setter
@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class BucketAggregationsStructure {

    @NonNull
    private String field;

    private String name;

    @NonNull
    private BucketType type = BucketType.TERMS;

    /**
     * 指定返回分组数量
     */
    private Integer size;

    private Sort sort;

    private List<Ranges> ranges;

    private LongBounds extendedBounds;

    /**
     * 时间格式
     */
    private String format;


    /**
     * 单位时间间隔
     *
     * @see DateHistogramInterval
     */
    private String interval;

    /**
     * 缺失值
     */
    private Object missingValue;

    private List<MetricsAggregationStructure> subMetricsAggregation = new LinkedList<>();

    private List<BucketAggregationsStructure> subBucketAggregation = new LinkedList<>();

    public String getName() {
        if (StringUtils.isNullOrEmpty(name)) {
            name = type.name().concat("_").concat(field);
        }
        return name;
    }
}

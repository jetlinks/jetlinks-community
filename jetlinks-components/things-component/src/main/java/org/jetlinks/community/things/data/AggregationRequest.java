package org.jetlinks.community.things.data;

import io.swagger.v3.oas.annotations.Hidden;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;
import org.hswebframework.ezorm.core.param.Term;
import org.hswebframework.ezorm.core.param.TermType;
import org.hswebframework.web.api.crud.entity.QueryParamEntity;
import org.jetlinks.community.Interval;
import org.jetlinks.community.doc.QueryConditionOnly;
import org.jetlinks.community.utils.ConverterUtils;
import org.jetlinks.reactor.ql.utils.CastUtils;
import org.joda.time.DateTime;

import javax.annotation.Nullable;
import java.util.Date;
import java.util.List;

@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class AggregationRequest {
    //时间间隔
    //为空时,不按时间分组
    @Schema(description = "间隔,如: 1d", type = "string", defaultValue = "1d")
    @Nullable
    @Builder.Default
    Interval interval = Interval.ofDays(1);

    //时间格式
    @Schema(defaultValue = "时间格式,如:yyyy-MM-dd", description = "yyyy-MM-dd")
    @Builder.Default
    String format = "yyyy-MM-dd";

    @Schema(description = "时间从,如: 2020-09-01 00:00:00,支持表达式: now-1d")
    @Builder.Default
    Date from = new DateTime()
        .plusMonths(-1)
        .withHourOfDay(0)
        .withMinuteOfHour(0)
        .withSecondOfMinute(0)
        .toDate();

    @Schema(description = "时间到,如: 2020-09-30 00:00:00,支持表达式: now-1d")
    @Builder.Default
    Date to = new DateTime()
        .withHourOfDay(23)
        .withMinuteOfHour(59)
        .withSecondOfMinute(59)
        .toDate();

    @Schema(description = "数量限制")
    Integer limit;

    //过滤条件
    @Schema(description = "过滤条件", implementation = QueryConditionOnly.class)
    @Builder.Default
    QueryParamEntity filter = QueryParamEntity.of();

    public int getLimit() {
        if (limit != null) {
            return limit;
        }
        if (interval == null) {
            return limit = 30;
        }
        long v = to.getTime() - from.getTime();

        limit = (int)Math.ceil(
            ((double) v / interval
                .getUnit()
                .getUnit()
                .getDuration()
                .toMillis())
        );

        return limit;
    }

    public AggregationRequest copy() {
        return new AggregationRequest(interval, format, from, to, limit, filter.clone());
    }

    @Hidden
    public void setQuery(QueryParamEntity filter) {
        setFilter(filter);
    }

    public void prepareTimestampCondition() {
        for (Term term : filter.getTerms()) {
            if ("timestamp".equals(term.getColumn())) {
                if (TermType.btw.equals(term.getTermType())) {
                    List<Object> values = ConverterUtils.convertToList(term.getValue());
                    if (!values.isEmpty()) {
                        from = CastUtils.castDate(values.get(0));
                    }
                    if (values.size() > 1) {
                        to = CastUtils.castDate(values.get(1));
                    }
                    term.setValue(null);
                } else if (TermType.gt.equals(term.getTermType()) || TermType.gte.equals(term.getTermType())) {
                    from = CastUtils.castDate(term.getValue());
                    term.setValue(null);
                } else if (TermType.lt.equals(term.getTermType()) || TermType.lte.equals(term.getTermType())) {
                    to = CastUtils.castDate(term.getValue());
                    term.setValue(null);
                }
            }
        }
    }
}
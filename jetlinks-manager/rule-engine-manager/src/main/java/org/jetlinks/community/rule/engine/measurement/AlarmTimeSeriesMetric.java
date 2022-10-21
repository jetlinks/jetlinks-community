package org.jetlinks.community.rule.engine.measurement;

import org.jetlinks.community.timeseries.TimeSeriesMetric;

/**
 * 媒体时序数据度量标识
 *
 * @author bestfeng
 *
 * @see org.jetlinks.pro.timeseries.TimeSeriesService
 * @see TimeSeriesMetric
 */
public interface AlarmTimeSeriesMetric {

    /**
     * 告警监控指标,用于对告警进行进行监控
     *
     * @return 度量标识
     */
    static TimeSeriesMetric alarmStreamMetrics() {
        return TimeSeriesMetric.of("alarm_metrics");
    }
}

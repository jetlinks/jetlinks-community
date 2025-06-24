package org.jetlinks.community.rule.engine.measurement;

import org.jetlinks.community.dashboard.supports.StaticMeasurementProvider;
import org.jetlinks.community.rule.engine.service.AlarmHistoryService;
import org.springframework.stereotype.Component;

/**
 * @author bestfeng
 */
@Component
public class AlarmRecordMeasurementProvider extends StaticMeasurementProvider {

//    MeterRegistry registry;

    public AlarmRecordMeasurementProvider(AlarmHistoryService historyService) {
        super(AlarmDashboardDefinition.alarm, AlarmObjectDefinition.record);

//        registry = registryManager.getMeterRegister(AlarmTimeSeriesMetric.alarmStreamMetrics().getId());
        addMeasurement(new AlarmRecordTrendMeasurement(historyService));
        addMeasurement(new AlarmRecordRankMeasurement(historyService));

    }

//    @EventListener
//    public void aggAlarmRecord(AlarmHistoryInfo info) {
//        registry
//            .counter("record-agg", getTags(info))
//            .increment();
//    }



}

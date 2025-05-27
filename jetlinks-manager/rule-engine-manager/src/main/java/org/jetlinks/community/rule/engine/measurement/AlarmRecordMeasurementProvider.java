package org.jetlinks.community.rule.engine.measurement;

import com.google.common.collect.Maps;
import io.micrometer.core.instrument.MeterRegistry;
import org.jetlinks.community.PropertyConstants;
import org.jetlinks.community.dashboard.supports.StaticMeasurementProvider;
import org.jetlinks.community.micrometer.MeterRegistryManager;
import org.jetlinks.community.rule.engine.alarm.AlarmConstants;
import org.jetlinks.community.rule.engine.entity.AlarmHistoryInfo;
import org.jetlinks.community.timeseries.TimeSeriesManager;
import org.jetlinks.community.utils.ConverterUtils;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * @author bestfeng
 */
@Component
public class AlarmRecordMeasurementProvider extends StaticMeasurementProvider {

    MeterRegistry registry;

    public AlarmRecordMeasurementProvider(MeterRegistryManager registryManager,
                                          TimeSeriesManager timeSeriesManager) {
        super(AlarmDashboardDefinition.alarm, AlarmObjectDefinition.record);

//        registry = registryManager.getMeterRegister(AlarmTimeSeriesMetric.alarmStreamMetrics().getId());
        addMeasurement(new AlarmRecordTrendMeasurement(timeSeriesManager));
        addMeasurement(new AlarmRecordRankMeasurement(timeSeriesManager));

    }

//    @EventListener
//    public void aggAlarmRecord(AlarmHistoryInfo info) {
//        registry
//            .counter("record-agg", getTags(info))
//            .increment();
//    }



    public String[] getTags(AlarmHistoryInfo info) {
        Map<String, Object> tagMap = Maps.newLinkedHashMap();

      //  tagMap.put(AlarmConstants.ConfigKey.targetId, info.getTargetId());
        //只需要记录targetType,用于统计 设备，产品等告警数量.
        tagMap.put(AlarmConstants.ConfigKey.targetType, info.getTargetType());
        //tagMap.put(AlarmConstants.ConfigKey.targetName, info.getTargetName());
//        tagMap.put(AlarmConstants.ConfigKey.alarmConfigId, info.getAlarmConfigId());
        tagMap.put(PropertyConstants.creatorId.getKey(), info.getCreatorId());
        return ConverterUtils.convertMapToTags(tagMap);
    }
}

/*
 * Copyright 2025 JetLinks https://www.jetlinks.cn
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
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

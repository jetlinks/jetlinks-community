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
package org.jetlinks.community.rule.engine.service.terms;

import org.jetlinks.community.terms.SubTableTermFragmentBuilder;
import org.springframework.stereotype.Component;

/**
 * 根据告警记录信息条件查询.
 * <p>
 * 例如：查询具有设备类型的告警记录的设备
 * <pre>{@code
 *     {
 *          "column":"id",
 *          "termType":"alarm-record",
 *          "value": [
 *              {
 *                  "column": "target_type",
 *                  "termType": "eq",
 *                  "value": "device"
 *              }
 *          ]
 *     }
 * }</pre>
 *
 * @author zhangji 2023/08/16
 */
@Component
public class AlarmRecordTerm extends SubTableTermFragmentBuilder {

    public AlarmRecordTerm() {
        super("alarm-record", "根据告警记录查询");
    }

    @Override
    protected String getSubTableName() {
        return "alarm_record";
    }

    @Override
    protected String getTableAlias() {
        return "_record";
    }

    @Override
    protected String getSubTableColumn() {
        return "target_id";
    }

}

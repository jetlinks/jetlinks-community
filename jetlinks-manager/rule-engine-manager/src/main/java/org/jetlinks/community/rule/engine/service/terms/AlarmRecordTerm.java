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

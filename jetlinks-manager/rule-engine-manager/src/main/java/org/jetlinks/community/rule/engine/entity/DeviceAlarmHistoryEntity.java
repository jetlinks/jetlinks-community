package org.jetlinks.community.rule.engine.entity;

import lombok.Getter;
import lombok.Setter;
import org.hswebframework.ezorm.rdb.mapping.annotation.ColumnType;
import org.hswebframework.ezorm.rdb.mapping.annotation.DefaultValue;
import org.hswebframework.ezorm.rdb.mapping.annotation.JsonCodec;
import org.hswebframework.web.api.crud.entity.GenericEntity;
import org.hswebframework.web.crud.generator.Generators;

import javax.persistence.Column;
import javax.persistence.Index;
import javax.persistence.Table;
import java.sql.JDBCType;
import java.util.Date;
import java.util.Map;

/**
 * 设备告警记录
 *
 * @author zhouhao
 * @since 1.1
 */
@Table(name = "rule_dev_alarm_history", indexes = {
    @Index(name = "idx_rahi_product_id", columnList = "product_id"),
    @Index(name = "idx_rahi_device_id", columnList = "device_id"),
    @Index(name = "idx_rahi_alarm_id", columnList = "alarm_id")
})
@Getter
@Setter
public class DeviceAlarmHistoryEntity extends GenericEntity<String> {

    @Column(length = 32, nullable = false, updatable = false)
    private String productId;

    @Column(updatable = false)
    private String productName;

    @Column(length = 32, nullable = false, updatable = false)
    private String deviceId;

    @Column(nullable = false, updatable = false)
    private String deviceName;

    @Column(length = 32, nullable = false, updatable = false)
    private String alarmId;

    @Column(nullable = false)
    private String alarmName;

    @Column
    @DefaultValue(generator = Generators.CURRENT_TIME)
    private Date alarmTime;

    @Column
    @ColumnType(javaType = String.class,jdbcType = JDBCType.CLOB)
    @JsonCodec
    private Map<String,Object> alarmData;

}

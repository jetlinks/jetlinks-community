package org.jetlinks.community.rule.engine.entity;

import io.swagger.v3.oas.annotations.media.Schema;
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

    @Column(length = 64, nullable = false, updatable = false)
    @Schema(description = "产品ID")
    private String productId;

    @Column(updatable = false)
    @Schema(description = "产品名称")
    private String productName;

    @Column(length = 64, nullable = false, updatable = false)
    @Schema(description = "设备ID")
    private String deviceId;

    @Column(nullable = false, updatable = false)
    @Schema(description = "设备名称")
    private String deviceName;

    @Column(length = 64, nullable = false, updatable = false)
    @Schema(description = "告警ID")
    private String alarmId;

    @Column(nullable = false)
    @Schema(description = "告警名称")
    private String alarmName;

    @Column
    @DefaultValue(generator = Generators.CURRENT_TIME)
    @Schema(description = "告警时间")
    private Date alarmTime;

    @Column
    @ColumnType(javaType = String.class, jdbcType = JDBCType.CLOB)
    @JsonCodec
    @Schema(description = "告警数据")
    private Map<String, Object> alarmData;

    @Column
    @DefaultValue("newer")
    @Schema(description = "状态", defaultValue = "newer")
    private String state;

    @Column
    @DefaultValue(generator = Generators.CURRENT_TIME)
    @Schema(description = "修改时间")
    private Date updateTime;

    @Column(length = 2000)
    @Schema(description = "说明", maxLength = 2000)
    private String description;

}

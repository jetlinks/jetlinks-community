package org.jetlinks.community.notify.manager.entity;

import com.alibaba.fastjson.JSON;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;
import org.hswebframework.ezorm.rdb.mapping.annotation.ColumnType;
import org.hswebframework.ezorm.rdb.mapping.annotation.DefaultValue;
import org.hswebframework.ezorm.rdb.mapping.annotation.EnumCodec;
import org.hswebframework.ezorm.rdb.mapping.annotation.JsonCodec;
import org.hswebframework.web.api.crud.entity.GenericEntity;
import org.hswebframework.web.bean.FastBeanCopier;
import org.hswebframework.web.crud.generator.Generators;
import org.jetlinks.community.notify.event.SerializableNotifierEvent;
import org.jetlinks.community.notify.manager.enums.NotifyState;
import org.jetlinks.community.notify.manager.service.NotifyHistory;

import javax.persistence.Column;
import javax.persistence.Index;
import javax.persistence.Table;
import java.sql.JDBCType;
import java.util.Date;
import java.util.Map;

@Table(name = "notify_history", indexes = {
    @Index(name = "idx_nt_his_notifier_id", columnList = "notifier_id")
})
@Getter
@Setter
public class NotifyHistoryEntity extends GenericEntity<String> {

    private static final long serialVersionUID = -6849794470754667710L;


    @Column(length = 32, nullable = false, updatable = false)
    @Schema(description = "通知ID")
    private String notifierId;

    @Column(nullable = false)
    @DefaultValue("success")
    @EnumCodec
    @ColumnType(javaType = String.class)
    @Schema(description = "状态")
    private NotifyState state;

    @Column(length = 1024)
    @Schema(description = "错误类型")
    private String errorType;

    @Column
    @ColumnType(jdbcType = JDBCType.CLOB, javaType = String.class)
    @Schema(description = "异常栈")
    private String errorStack;

    @Column(length = 32, nullable = false)
    @DefaultValue("-")
    @Schema(description = "模版ID")
    private String templateId;

    @Column
    @ColumnType(jdbcType = JDBCType.CLOB, javaType = String.class)
    @Schema(description = "模版内容")
    private String template;

    @Column
    @ColumnType(jdbcType = JDBCType.CLOB, javaType = String.class)
    @JsonCodec
    @Schema(description = "上下文")
    private Map<String, Object> context;

    @Column(length = 32, nullable = false)
    @Schema(description = "服务商")
    private String provider;

    @Column(length = 32, nullable = false)
    @Schema(description = "通知类型")
    private String notifyType;

    @Column
    @DefaultValue(generator = Generators.CURRENT_TIME)
    @Schema(description = "通知时间")
    private Date notifyTime;

    @Column
    @DefaultValue("0")
    @Schema(description = "重试次数")
    private Integer retryTimes;

    public static NotifyHistoryEntity of(SerializableNotifierEvent event) {
       NotifyHistoryEntity entity = FastBeanCopier.copy(event, new NotifyHistoryEntity());
        if (null != event.getTemplate()) {
            entity.setTemplate(JSON.toJSONString(event.getTemplate()));
        }
        if (event.isSuccess()) {
            entity.setState(NotifyState.success);
        } else {
            entity.setErrorStack(event.getCause());
            entity.setState(NotifyState.error);
        }
        return entity;
    }

    public NotifyHistory toHistory(){
        return FastBeanCopier.copy(this,new NotifyHistory());
    }
}

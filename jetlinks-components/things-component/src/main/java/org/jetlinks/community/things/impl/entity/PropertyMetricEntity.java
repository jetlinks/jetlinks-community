package org.jetlinks.community.things.impl.entity;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;
import org.hswebframework.ezorm.rdb.mapping.annotation.DefaultValue;
import org.hswebframework.web.api.crud.entity.GenericEntity;
import org.hswebframework.web.api.crud.entity.RecordCreationEntity;
import org.hswebframework.web.crud.annotation.EnableEntityEvent;
import org.hswebframework.web.crud.generator.Generators;
import org.hswebframework.web.utils.DigestUtils;
import org.jetlinks.community.PropertyMetric;

import javax.persistence.Column;
import javax.persistence.Table;

@Getter
@Setter
@Table(name = "thing_property_metric")
@EnableEntityEvent
public class PropertyMetricEntity extends GenericEntity<String> implements RecordCreationEntity {

    @Schema(description = "物类型,如: device")
    @Column(length = 32, nullable = false, updatable = false)
    private String thingType;

    @Schema(description = "物ID,如: 设备ID")
    @Column(length = 64, nullable = false, updatable = false)
    private String thingId;

    @Schema(description = "属性标识")
    @Column(length = 64, nullable = false, updatable = false)
    private String property;

    @Schema(description = "指标标识")
    @Column(length = 64, nullable = false, updatable = false)
    private String metric;

    @Schema(description = "指标名称")
    @Column(length = 64)
    private String metricName;

    @Schema(description = "指标值")
    @Column(length = 256, nullable = false)
    private String value;

    @Schema(description = "是否为范围值")
    @Column(nullable = false)
    @DefaultValue("false")
    private Boolean range;

    @Schema(description = "创建人ID")
    @Column(length = 64, nullable = false, updatable = false)
    private String creatorId;

    @Schema(description = "创建时间")
    @Column(nullable = false, updatable = false)
    @DefaultValue(generator = Generators.CURRENT_TIME)
    private Long createTime;

    public void genericId(){
        setId(genericId(
            thingType,thingId,property,metric
        ));
    }
    public static String genericId(String thingType, String thingId,String property, String metric) {
        return DigestUtils.md5Hex(String.join("|", thingType, thingId, property, metric));
    }

    public PropertyMetric toMetric(){
        PropertyMetric propertyMetric=new PropertyMetric();
        propertyMetric.setId(metric);
        propertyMetric.setValue(value);
        propertyMetric.setRange(range);
        propertyMetric.setName(metricName);
        return propertyMetric;
    }
}

package org.jetlinks.community.visualization.entity;

import lombok.Data;
import lombok.EqualsAndHashCode;
import org.hswebframework.ezorm.rdb.mapping.annotation.ColumnType;
import org.hswebframework.ezorm.rdb.mapping.annotation.DefaultValue;
import org.hswebframework.ezorm.rdb.mapping.annotation.EnumCodec;
import org.hswebframework.web.api.crud.entity.GenericEntity;
import org.jetlinks.community.visualization.enums.DataVisualizationState;

import javax.persistence.Column;
import javax.persistence.Index;
import javax.persistence.Table;
import javax.validation.constraints.NotBlank;
import java.sql.JDBCType;

/**
 * 数据可视化
 *
 * @author zhouhao
 * @since 1.1
 */
@Table(name = "vis_data_visualization", indexes = {
    @Index(name = "idx_vis_type_target", columnList = "type,target")
})
@EqualsAndHashCode(callSuper = true)
@Data
public class DataVisualizationEntity extends GenericEntity<String> {

    @Column(length = 32, nullable = false, updatable = false)
    @NotBlank(message = "[type]不能为空")
    private String type;

    @Column(length = 32, nullable = false, updatable = false)
    @NotBlank(message = "[target]不能为空")
    private String target;

    @Column
    private String name;

    @Column
    private String description;

    @Column
    @ColumnType(jdbcType = JDBCType.CLOB)
    private String metadata;

    @Column(length = 32, nullable = false)
    @EnumCodec
    @ColumnType(javaType = String.class)
    @DefaultValue("enabled")
    private DataVisualizationState state;

    public void applyId() {
        this.setId(String.format("%s:%s", type, target));
    }

    public static DataVisualizationEntity newEmpty(String type, String target) {
        DataVisualizationEntity entity = new DataVisualizationEntity();
        entity.setType(type);
        entity.setTarget(target);
        entity.applyId();
        entity.setMetadata("");
        return entity;
    }

}

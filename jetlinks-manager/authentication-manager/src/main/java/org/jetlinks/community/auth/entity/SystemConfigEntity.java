package org.jetlinks.community.auth.entity;

import lombok.*;
import org.hswebframework.ezorm.rdb.mapping.annotation.ColumnType;
import org.hswebframework.ezorm.rdb.mapping.annotation.JsonCodec;
import org.hswebframework.web.api.crud.entity.GenericEntity;

import javax.persistence.Column;
import javax.persistence.Table;
import java.sql.JDBCType;
import java.util.Map;

@Getter
@Setter
@Table(name = "s_system_conf")
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SystemConfigEntity extends GenericEntity<String> {

    /**
     * 前端配置
     */
    @Column
    @ColumnType(jdbcType = JDBCType.CLOB)
    @JsonCodec
    private Map<String, Object> frontConfig;

    public static SystemConfigEntity front(String id,Map<String, Object> front){
        SystemConfigEntity entity=new SystemConfigEntity();
        entity.setId(id);
        entity.setFrontConfig(front);
        return entity;
    }

}

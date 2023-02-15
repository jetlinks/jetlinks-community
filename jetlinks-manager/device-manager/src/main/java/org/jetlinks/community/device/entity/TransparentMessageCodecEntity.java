package org.jetlinks.community.device.entity;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;
import org.apache.commons.codec.digest.DigestUtils;
import org.hswebframework.ezorm.rdb.mapping.annotation.ColumnType;
import org.hswebframework.ezorm.rdb.mapping.annotation.DefaultValue;
import org.hswebframework.ezorm.rdb.mapping.annotation.JsonCodec;
import org.hswebframework.web.api.crud.entity.GenericEntity;
import org.hswebframework.web.api.crud.entity.RecordCreationEntity;
import org.hswebframework.web.api.crud.entity.RecordModifierEntity;
import org.hswebframework.web.crud.annotation.EnableEntityEvent;
import org.hswebframework.web.crud.generator.Generators;
import org.jetlinks.community.device.message.transparent.TransparentMessageCodecProvider;
import org.springframework.util.StringUtils;

import javax.persistence.Column;
import javax.persistence.Table;
import java.sql.JDBCType;
import java.util.Map;

@Getter
@Setter
@Table(name = "dev_transparent_codec")
@Schema(description = "透传消息解析器")
@EnableEntityEvent
public class TransparentMessageCodecEntity extends GenericEntity<String> implements RecordCreationEntity, RecordModifierEntity {

    @Schema(description = "产品ID")
    @Column(length = 64, nullable = false, updatable = false)
    private String productId;

    @Schema(description = "设备ID")
    @Column(length = 64, updatable = false)
    private String deviceId;

    /**
     * @see TransparentMessageCodecProvider#getProvider()
     */
    @Schema(description = "编解码器提供商,如: jsr223")
    @Column(length = 64, nullable = false)
    private String provider;

    /**
     * 编解码配置
     *
     * @see TransparentMessageCodecProvider#createCodec(Map)
     */
    @Schema(description = "编解码配置")
    @Column(nullable = false)
    @ColumnType(jdbcType = JDBCType.LONGVARCHAR, javaType = String.class)
    @JsonCodec
    private Map<String, Object> configuration;

    @Schema(description = "创建人ID")
    @Column(length = 64, nullable = false, updatable = false)
    private String creatorId;

    @Schema(description = "创建时间")
    @Column(updatable = false)
    @DefaultValue(generator = Generators.CURRENT_TIME)
    private Long createTime;

    @Schema(description = "修改人ID")
    @Column(length = 64)
    private String modifierId;

    @Schema(description = "修改时间")
    @DefaultValue(generator = Generators.CURRENT_TIME)
    private Long modifyTime;

    @Override
    public String getId() {
        if (!StringUtils.hasText(super.getId())) {
            super.setId(
                createId(productId, deviceId)
            );
        }
        return super.getId();
    }

    public static String createId(String productId, String deviceId) {
        return DigestUtils.md5Hex(String.join("|", productId, deviceId));
    }
}

package org.jetlinks.community.configure.device;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Generated;
import lombok.Getter;
import lombok.Setter;
import org.apache.commons.codec.binary.Base64;
import org.hswebframework.ezorm.rdb.mapping.annotation.ColumnType;
import org.hswebframework.web.api.crud.entity.GenericEntity;
import org.jetlinks.core.device.DeviceRegistry;
import org.jetlinks.core.server.session.DeviceSessionProvider;
import org.jetlinks.core.server.session.PersistentSession;
import org.springframework.util.StringUtils;
import reactor.core.publisher.Mono;

import javax.persistence.Column;
import java.sql.JDBCType;

@Getter
@Setter
@Generated
public class PersistentSessionEntity extends GenericEntity<String> {

    /**
     * @see DeviceSessionProvider#getId()
     */
    @Schema(description="设备会话提供商")
    @Column(length = 32, nullable = false)
    private String provider;

    @Schema(description="设备连接的网关服务ID")
    @Column(length = 64, nullable = false)
    private String serverId;

    @Schema(description="设备ID")
    @Column(length = 64, nullable = false)
    private String deviceId;

    @Schema(description="会话超时时间")
    @Column
    private Long keepAliveTimeout;

    @Schema(description="最近会话时间")
    @Column
    private Long lastKeepAliveTime;

    @Schema(description="会话序列化")
    @Column
    @ColumnType(javaType = String.class, jdbcType = JDBCType.LONGVARCHAR)
    private String sessionBase64;

    public static Mono<PersistentSessionEntity> from(String serverId,
                                                     PersistentSession session,
                                                     DeviceRegistry registry) {
        PersistentSessionEntity entity = new PersistentSessionEntity();

        entity.setId(session.getId());
        entity.setProvider(session.getProvider());
        entity.setServerId(serverId);
        entity.setDeviceId(session.getDeviceId());
        entity.setKeepAliveTimeout(session.getKeepAliveTimeout().toMillis());
        entity.setLastKeepAliveTime(session.lastPingTime());
        DeviceSessionProvider provider = DeviceSessionProvider
            .lookup(session.getProvider())
            .orElseGet(UnknownDeviceSessionProvider::getInstance);

        return provider
            .serialize(session, registry)
            .map(Base64::encodeBase64String)
            .doOnNext(entity::setSessionBase64)
            .thenReturn(entity);

    }

    public Mono<PersistentSession> toSession(DeviceRegistry registry) {
        DeviceSessionProvider provider = DeviceSessionProvider
            .lookup(getProvider())
            .orElseGet(UnknownDeviceSessionProvider::getInstance);

        if (StringUtils.hasText(sessionBase64)) {
            return provider.deserialize(Base64.decodeBase64(sessionBase64), registry);
        }
        return Mono.empty();
    }
}

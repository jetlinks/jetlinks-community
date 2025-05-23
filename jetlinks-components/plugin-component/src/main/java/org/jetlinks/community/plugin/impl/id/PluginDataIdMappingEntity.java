package org.jetlinks.community.plugin.impl.id;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;
import org.apache.commons.lang3.StringUtils;
import org.hswebframework.web.api.crud.entity.GenericEntity;
import org.hswebframework.web.crud.annotation.EnableEntityEvent;
import org.hswebframework.web.utils.DigestUtils;

import javax.persistence.Column;
import javax.persistence.Index;
import javax.persistence.Table;

@Getter
@Setter
@Table(name = "s_plugin_id_mapping", indexes = {
    @Index(name = "idx_plg_im_internal", columnList = "type,pluginId,internalId"),
    @Index(name = "idx_plg_im_external", columnList = "type,pluginId,externalId"),
})
@Schema(description = "插件数据ID映射表")
@EnableEntityEvent
public class PluginDataIdMappingEntity extends GenericEntity<String> {

    @Column(length = 32, updatable = false, nullable = false)
    @Schema(description = "数据类型")
    private String type;

    @Column(length = 32, nullable = false, updatable = false)
    @Schema(description = "插件ID")
    private String pluginId;

    @Column(length = 64, updatable = false, nullable = false)
    @Schema(description = "内部数据ID")
    private String internalId;

    @Column(length = 64, updatable = false, nullable = false)
    @Schema(description = "插件数据ID")
    private String externalId;

    @Override
    public String getId() {
        if (StringUtils.isEmpty(super.getId())) {
            setId(
                DigestUtils.md5Hex(
                    String.join("|", type, pluginId, internalId, externalId)
                )
            );
        }
        return super.getId();
    }

    public static PluginDataIdMappingEntity of(String pluginId,
                                               String internalId,
                                               String type,
                                               String externalId) {
        PluginDataIdMappingEntity entity = new PluginDataIdMappingEntity();
        entity.setPluginId(pluginId);
        entity.setType(type);
        entity.setInternalId(internalId);
        entity.setExternalId(externalId);
        return entity;
    }
}

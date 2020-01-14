package org.jetlinks.community.elastic.search.index;

import lombok.Getter;
import lombok.Setter;
import org.elasticsearch.client.indices.CreateIndexRequest;
import org.elasticsearch.common.settings.Settings;
import org.jetlinks.community.elastic.search.index.mapping.MappingFactory;
import org.jetlinks.community.elastic.search.index.setting.SettingFactory;

import java.util.Collections;
import java.util.Map;

/**
 * @author bsetfeng
 * @since 1.0
 **/

public class CreateIndex {

    @Getter
    @Setter
    private Map<String, Object> mapping;

    @Getter
    @Setter
    private Settings.Builder settings;

    private String index;

    @Deprecated
    private String type;

    public CreateIndex addIndex(String index) {
        this.index = index;
        return this;
    }

    @Deprecated
    public CreateIndex addType(String type) {
        this.type = type;
        return this;
    }

    public MappingFactory createMapping() {
        return MappingFactory.createInstance(this);
    }

    public SettingFactory createSettings() {
        return SettingFactory.createInstance(this);
    }


    public CreateIndexRequest createIndexRequest() {
        CreateIndexRequest request = new CreateIndexRequest(index);
        request.mapping(Collections.singletonMap("properties", getMapping()));
        if (settings != null) {
            request.settings(settings);
        }
        return request;
    }

    private CreateIndex() {
    }

    public static CreateIndex createInstance() {
        return new CreateIndex();
    }
}

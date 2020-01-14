package org.jetlinks.community.elastic.search.index.setting;

import org.elasticsearch.common.settings.Settings;
import org.jetlinks.community.elastic.search.index.CreateIndex;

/**
 * @author bsetfeng
 * @since 1.0
 **/
public class SettingFactory {

    private Settings.Builder settings = Settings.builder();

    private CreateIndex index;

    private SettingFactory(CreateIndex index) {
        this.index = index;
    }

    public SettingFactory settingShards(Integer shards) {
        settings.put("number_of_shards", shards);
        return this;
    }

    public SettingFactory settingReplicas(Integer replicas) {
        settings.put("number_of_replicas", replicas);
        return this;
    }

    public CreateIndex end() {
        index.setSettings(settings);
        return index;
    }

    public static SettingFactory createInstance(CreateIndex index) {
        return new SettingFactory(index);
    }


}

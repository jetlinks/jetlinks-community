package org.jetlinks.community.elastic.search.embedded;

import lombok.Getter;
import lombok.Setter;
import org.elasticsearch.common.settings.Settings;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "elasticsearch.embedded")
@Getter
@Setter
public class EmbeddedElasticSearchProperties {

    private boolean enabled;

    private String dataPath = "./data/elasticsearch";

    private String homePath = "./";

    private int port = 9200;

    private String host = "0.0.0.0";


    public Settings.Builder applySetting(Settings.Builder settings) {
        return settings.put("network.host", host)
            .put("http.port", port)
            .put("path.data", dataPath)
            .put("path.home", homePath);
    }
}

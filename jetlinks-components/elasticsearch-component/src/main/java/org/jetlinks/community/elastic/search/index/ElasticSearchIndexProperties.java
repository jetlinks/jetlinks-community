package org.jetlinks.community.elastic.search.index;

import lombok.*;
import org.elasticsearch.common.settings.Settings;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@ConfigurationProperties(prefix = "elasticsearch.index.settings")
public class ElasticSearchIndexProperties {

    private int numberOfShards = 1;

    private int numberOfReplicas = 0;

    public Settings toSettings() {

        return Settings.builder()
            .put("number_of_shards", Math.max(1, numberOfShards))
            .put("number_of_replicas", numberOfReplicas)
            .build();
    }
}

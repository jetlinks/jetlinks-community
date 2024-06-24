package org.jetlinks.community.elastic.search.index;

import lombok.*;
import org.elasticsearch.common.settings.Settings;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@ConfigurationProperties(prefix = "elasticsearch.index.settings")
public class ElasticSearchIndexProperties {

    private int numberOfShards = 1;

    private int numberOfReplicas = 0;

    private Map<String,String> options = new HashMap<>();

    public Settings toSettings() {

        return Settings.builder()
            .put("number_of_shards", Math.max(1, numberOfShards))
            .put("number_of_replicas", numberOfReplicas)
            .putProperties(options, Function.identity())
            .build();
    }
}

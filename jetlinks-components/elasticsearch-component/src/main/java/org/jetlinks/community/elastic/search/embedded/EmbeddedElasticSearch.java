package org.jetlinks.community.elastic.search.embedded;

import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.node.InternalSettingsPreparer;
import org.elasticsearch.node.Node;
import org.elasticsearch.transport.Netty4Plugin;

import java.util.Collections;

@Slf4j
public class EmbeddedElasticSearch extends Node {

    static {
        System.setProperty("es.set.netty.runtime.available.processors","false");
    }
    @SneakyThrows
    public EmbeddedElasticSearch(EmbeddedElasticSearchProperties properties) {
        super(InternalSettingsPreparer.prepareEnvironment(
            properties.applySetting(
                Settings.builder()
                    .put("node.name", "test")
                    .put("discovery.type", "single-node")
                    .put("transport.type", "netty4")
                    .put("http.type", "netty4")
                    .put("network.host", "0.0.0.0")
                    .put("http.port", 9200)
            ).build(), null),
            Collections.singleton(Netty4Plugin.class), false);
    }

    @Override
    protected void registerDerivedNodeNameWithLogger(String nodeName) {

    }

}

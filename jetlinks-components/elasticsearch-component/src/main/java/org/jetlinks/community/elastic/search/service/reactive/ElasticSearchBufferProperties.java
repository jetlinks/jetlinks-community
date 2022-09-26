package org.jetlinks.community.elastic.search.service.reactive;

import lombok.Getter;
import lombok.Setter;
import org.jetlinks.community.buffer.BufferProperties;
import org.jetlinks.community.buffer.BufferProperties;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Getter
@Setter
@ConfigurationProperties(prefix = "elasticsearch.buffer")
public class ElasticSearchBufferProperties extends BufferProperties {
    public ElasticSearchBufferProperties() {
        //固定缓冲文件目录
        setFilePath("./data/elasticsearch-buffer");
        setSize(3000);
    }

    private boolean refreshWhenWrite = false;
}
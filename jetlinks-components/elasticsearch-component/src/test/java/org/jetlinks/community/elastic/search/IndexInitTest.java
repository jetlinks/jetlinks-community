package org.jetlinks.community.elastic.search;

import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.http.HttpHost;
import org.elasticsearch.client.RestClient;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.client.indices.CreateIndexRequest;
import org.jetlinks.community.elastic.search.enums.FieldDateFormat;
import org.jetlinks.community.elastic.search.enums.FieldType;
import org.jetlinks.community.elastic.search.index.CreateIndex;
import org.jetlinks.community.elastic.search.index.DefaultIndexOperationService;
import org.jetlinks.community.elastic.search.index.mapping.IndicesMappingCenter;
import org.jetlinks.community.elastic.search.service.IndexOperationService;
import org.junit.jupiter.api.Test;
import reactor.test.StepVerifier;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Date;

/**
 * @author bsetfeng
 * @since 1.0
 **/
@Slf4j
public class IndexInitTest {
    RestHighLevelClient client = new RestHighLevelClient(
            RestClient.builder(new HttpHost("localhost", 9200, "http")));
    IndexOperationService operationService =
            new DefaultIndexOperationService(new ElasticRestClient(client,client),new IndicesMappingCenter());

    @Test
    @SneakyThrows
    public void simpleTest() {

        CreateIndexRequest request = CreateIndex.createInstance()
                .addIndex("bestfeng")
                .createMapping()
                .addFieldName("date").addFieldType(FieldType.DATE).addFieldDateFormat(FieldDateFormat.epoch_millis, FieldDateFormat.simple_date).commit()
                .addFieldName("name").addFieldType(FieldType.KEYWORD).commit()
                .end()
                .createSettings()
                .settingReplicas(2)
                .settingShards(6)
                .end()
                .createIndexRequest();
        operationService.init(request)
                .as(StepVerifier::create)
                .expectNextMatches(bool -> bool)
                .verifyComplete();
    }

    public static void main(String[] args) {
        Date date2 = new Date(1575528676826L);
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        LocalDateTime localDateTime2 = LocalDateTime.ofInstant(date2.toInstant(), ZoneId.systemDefault());
        System.out.println(localDateTime2.format(formatter));
    }
}

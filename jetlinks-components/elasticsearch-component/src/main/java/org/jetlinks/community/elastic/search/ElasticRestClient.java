package org.jetlinks.community.elastic.search;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.elasticsearch.client.RestHighLevelClient;

/**
 * @author bsetfeng
 * @since 1.0
 **/
@AllArgsConstructor
@NoArgsConstructor
@Getter
public class ElasticRestClient {

    private RestHighLevelClient queryClient;

    private RestHighLevelClient writeClient;
}

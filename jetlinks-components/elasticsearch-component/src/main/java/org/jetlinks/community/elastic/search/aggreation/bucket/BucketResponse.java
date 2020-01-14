package org.jetlinks.community.elastic.search.aggreation.bucket;

import lombok.*;

import java.util.List;

/**
 * @author bsetfeng
 * @since 1.0
 **/
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class BucketResponse {

    private String name;

    private List<Bucket> buckets;
}

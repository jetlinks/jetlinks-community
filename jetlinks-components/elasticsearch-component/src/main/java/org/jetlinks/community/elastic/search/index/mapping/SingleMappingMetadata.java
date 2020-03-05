package org.jetlinks.community.elastic.search.index.mapping;

import lombok.*;
import org.jetlinks.community.elastic.search.enums.ElasticDateFormat;
import org.jetlinks.community.elastic.search.enums.ElasticPropertyType;

/**
 * @author bsetfeng
 * @since 1.0
 **/
@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class SingleMappingMetadata {

    private String name;

    private ElasticDateFormat format;

    private ElasticPropertyType type;
}

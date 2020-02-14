package org.jetlinks.community.elastic.search.index.mapping;

import lombok.*;
import org.jetlinks.community.elastic.search.enums.FieldDateFormat;
import org.jetlinks.community.elastic.search.enums.FieldType;

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

    private FieldDateFormat format;

    private FieldType type;
}

package org.jetlinks.community.elastic.search.manager.entity;

import lombok.*;

/**
 * @author bsetfeng
 * @since 1.0
 **/
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class IndexStrategy {

    private String strategyName;

    private String patternName;
}

package org.jetlinks.community.auth.dimension;

import lombok.AllArgsConstructor;
import lombok.Generated;
import lombok.Getter;
import org.hswebframework.web.authorization.DimensionType;

/**
 * @author wangzheng
 * @since 1.0
 */
@AllArgsConstructor
@Getter
@Generated
public enum OrgDimensionType implements DimensionType {
    org("org","机构");

    private final String id;
    private final String name;

}

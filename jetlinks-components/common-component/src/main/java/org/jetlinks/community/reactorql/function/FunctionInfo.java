package org.jetlinks.community.reactorql.function;

import lombok.Getter;
import lombok.Setter;
import org.jetlinks.core.metadata.DataType;
import org.jetlinks.core.metadata.PropertyMetadata;

import java.util.List;

/**
 *
 * @author zhangji 2025/1/22
 * @since 2.3
 */
@Getter
@Setter
public class FunctionInfo {
    private String id;

    private String name;

    private DataType outputType;

    private List<PropertyMetadata> args;
}

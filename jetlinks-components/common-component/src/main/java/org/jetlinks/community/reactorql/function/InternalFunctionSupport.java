package org.jetlinks.community.reactorql.function;

import com.google.common.collect.Sets;
import lombok.AllArgsConstructor;
import org.hswebframework.ezorm.rdb.operator.builder.fragments.SqlFragments;
import org.jetlinks.core.metadata.DataType;
import org.jetlinks.core.metadata.types.ArrayType;
import org.jetlinks.core.metadata.types.LongType;

import java.util.Collections;
import java.util.Map;
import java.util.Set;

/**
 *
 * @author zhangji 2025/1/22
 * @since 2.3
 */
@AllArgsConstructor
public enum InternalFunctionSupport implements FunctionSupport {

    array_len("集合长度", LongType.GLOBAL, ArrayType.ID);

    private final String name;
    private final DataType outputType;
    private final Set<String> supportTypes;

    static {
        for (InternalFunctionSupport value : values()) {
            InternalFunctionSupport.supports.register(value.getId(), value);
        }
    }

    public static void register(){

    }

    InternalFunctionSupport(String name, DataType outputType, String... supportTypes) {
        this.name = name;
        this.outputType = outputType;
        this.supportTypes = Collections.unmodifiableSet(
            Sets.newHashSet(supportTypes)
        );
    }

    @Override
    public String getId() {
        return name();
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public boolean isSupported(DataType type) {
        return supportTypes.contains(type.getId());
    }

    @Override
    public DataType getOutputType() {
        return outputType;
    }

    @Override
    public SqlFragments createSql(String column, Map<String, Object> args) {
        return SqlFragments.of(getId() + "(", column, ")");
    }
}

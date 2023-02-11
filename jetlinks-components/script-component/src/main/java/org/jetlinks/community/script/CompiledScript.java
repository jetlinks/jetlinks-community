package org.jetlinks.community.script;

import org.jetlinks.community.script.context.ExecutionContext;

import java.util.Collections;
import java.util.Map;

/**
 * 已编译的脚本信息.
 *
 * @author zhouhao
 * @since 2.0
 */
public interface CompiledScript {

    /**
     * 使用指定上下文执行脚本
     *
     * @param context 上下文
     * @return 脚本返回结果
     */
    Object call(ExecutionContext context);

    default Object call(Map<String, Object> context) {
        return call(ExecutionContext.create(context));
    }

    default Object call() {
        return call(Collections.emptyMap());
    }

}

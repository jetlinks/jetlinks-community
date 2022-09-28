package org.jetlinks.community.relation.utils;

import org.jetlinks.core.config.ConfigKey;
import reactor.core.publisher.Flux;

import java.util.Map;

/**
 * 动态变量工具类,对动态变量获取的支持.
 * <p>
 * 通过在上下文中设置值为{@link  VariableSource},来表示此变量值为动态变量.
 *
 * @author zhouhao
 * @see VariableSource
 * @since 2.0
 */
public class RelationUtils {

    /**
     * 从上下文中解析出值,上下文支持通过动态变量{@link VariableSource}来指定值的来源.
     *
     * <pre>{@code
     *
     * RelationUtils.resolve("phoneNumber",context,RelationConstants.UserProperty.telephone)
     *
     * }</pre>
     *
     * @param key                  key
     * @param context              上下文
     * @param relationPropertyPath 关系对象属性
     * @return 值
     */
    public static Flux<Object> resolve(String key,
                                       Map<String, Object> context,
                                       ConfigKey<String> relationPropertyPath) {
        return VariableSource.resolveValue(key, context, relationPropertyPath);

    }
}

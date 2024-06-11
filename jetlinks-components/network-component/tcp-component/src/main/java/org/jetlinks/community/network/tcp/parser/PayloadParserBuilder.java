package org.jetlinks.community.network.tcp.parser;

import org.jetlinks.community.ValueObject;

import java.util.function.Supplier;

/**
 * 解析器构造器，用于根据解析器类型和配置信息构造对应的解析器
 *
 * @author zhouhao
 * @since 1.0
 */
public interface PayloadParserBuilder {

    /**
     * 构造解析器
     *
     * @param type          解析器类型
     * @param configuration 配置信息
     * @return 解析器
     */
    Supplier<PayloadParser> build(PayloadParserType type, ValueObject configuration);

}

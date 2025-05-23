/*
 * Copyright 2025 JetLinks https://www.jetlinks.cn
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.jetlinks.community.reactorql.function;

import org.hswebframework.ezorm.rdb.operator.builder.fragments.SqlFragments;
import org.jetlinks.core.metadata.DataType;
import org.jetlinks.community.spi.Provider;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 函数支持,用于定义在可以在ReactorQL中使用的函数.
 *
 * @author zhangji 2025/1/22
 * @since 2.3
 */
public interface FunctionSupport {

    Provider<FunctionSupport> supports = Provider.create(FunctionSupport.class);

    String getId();

    String getName();

    /**
     * 是否支持列的数据类型
     *
     * @param type 数据类型
     * @return 是否支持
     */
    boolean isSupported(DataType type);

    /**
     * 获取输出数据类型
     *
     * @return 输出数据类型
     */
    DataType getOutputType();

    /**
     * 创建SQL函数片段
     *
     * @param column 列名
     * @param args   参数
     * @return SQL函数片段
     */
    SqlFragments createSql(String column, Map<String, Object> args);

    /**
     * 查找支持的函数
     *
     * @param type 数据类型
     * @return 函数信息
     */
    static List<FunctionInfo> lookup(DataType type) {
        return supports
            .getAll()
            .stream()
            .filter(support -> support.isSupported(type))
            .map(FunctionSupport::toInfo)
            .collect(Collectors.toList());
    }


    default FunctionInfo toInfo() {
        FunctionInfo info = new FunctionInfo();
        info.setId(getId());
        info.setOutputType(getOutputType());
        info.setName(getName());
        return info;
    }
}

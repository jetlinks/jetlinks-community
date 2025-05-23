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
package org.jetlinks.community.dictionary;

import org.hswebframework.web.dict.EnumDict;

import javax.annotation.Nonnull;
import java.util.List;
import java.util.Optional;

/**
 * 数据字典管理器,用于获取数据字典的枚举值
 *
 * @author zhouhao
 * @since 2.1
 */
public interface DictionaryManager {

    /**
     * 获取字典的所有选项
     *
     * @param dictId 字典ID
     * @return 字典值
     */
    @Nonnull
    List<EnumDict<?>> getItems(@Nonnull String dictId);

    /**
     * 获取字段选项
     *
     * @param dictId 字典ID
     * @param itemId 选项ID
     * @return 选项值
     */
    @Nonnull
    Optional<EnumDict<?>> getItem(@Nonnull String dictId,
                                  @Nonnull String itemId);





}

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
package org.jetlinks.community;

import org.jetlinks.core.config.ConfigKey;
import org.jetlinks.core.metadata.MergeOption;

import java.util.Map;

/**
 * 数据验证配置常量类
 *
 * @author zhouhao
 * @see ConfigKey
 */
public interface ConfigMetadataConstants {

    //字符串相关配置
    ConfigKey<Long> maxLength = ConfigKey.of("maxLength", "字符串最大长度", Long.TYPE);
    ConfigKey<Boolean> isRichText = ConfigKey.of("isRichText", "是否为富文本", Boolean.TYPE);
    ConfigKey<Boolean> isScript = ConfigKey.of("isScript", "是否为脚本", Boolean.TYPE);

    ConfigKey<Boolean> allowInput = ConfigKey.of("allowInput", "允许输入", Boolean.TYPE);
    ConfigKey<Boolean> required = ConfigKey.of("required", "是否必填", Boolean.TYPE);

    ConfigKey<String> format = ConfigKey.of("format", "格式", String.class);

    ConfigKey<String> defaultValue = ConfigKey.of("defaultValue", "默认值", String.class);

    ConfigKey<Boolean> indexEnabled = ConfigKey.of("indexEnabled", "开启索引", Boolean.TYPE);

}

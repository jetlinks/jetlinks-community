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
package org.jetlinks.community.io.file;

import org.hswebframework.web.dict.EnumDict;
import org.springframework.util.StringUtils;

public enum FileOption implements EnumDict<String> {

    /**
     * 公开访问
     */
    publicAccess,

    /**
     * 临时文件,将会被定时删除
     */
    tempFile;

    public static final FileOption[] all = FileOption.values();

    public static FileOption[] parse(String str) {
        if (!StringUtils.hasText(str)) {
            return new FileOption[0];
        }

        String[] arr = str.split(",");
        FileOption[] options = new FileOption[arr.length];

        for (int i = 0; i < arr.length; i++) {
            options[i] = FileOption.valueOf(arr[i]);
        }
        return options;
    }

    @Override
    public String getValue() {
        return name();
    }

    @Override
    public String getText() {
        return name();
    }
}

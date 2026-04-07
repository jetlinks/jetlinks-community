/*
 * Copyright 2026 JetLinks https://www.jetlinks.cn
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
package org.jetlinks.community.io.excel.converter;

import org.hswebframework.reactor.excel.ExcelHeader;
import org.hswebframework.web.bean.FastBeanCopier;
import org.hswebframework.web.dict.EnumDict;
import org.hswebframework.web.dict.defaults.DefaultItemDefine;
import org.jetlinks.community.dictionary.Dictionaries;

public class DictionaryConverter implements ConverterExcelOption {
    private final String dictId;

    private final Class<?> type;

    public DictionaryConverter(String dictId, Class<?> type) {
        this.dictId = dictId;
        this.type = type;
    }

    @Override
    public Object convertForWrite(Object val, ExcelHeader header) {
        if (val instanceof EnumDict) {
            String text = ((EnumDict<?>) val).getText();
            if (null == text) {
                text = String.valueOf(((EnumDict<?>) val).getValue());
            }
            return text;
        }
        return Dictionaries
            .findItem(dictId, val)
            .map(EnumDict::getText)
            .orElse("");
    }

    @Override
    public Object convertForRead(Object cell, ExcelHeader header) {
        EnumDict<?> dict = Dictionaries
            .findItem(dictId, cell)
            .orElse(null);
        if (dict == null) {
            if (EnumDict.class == type) {
                return DefaultItemDefine
                    .builder()
                    .text(String.valueOf(cell))
                    .build();
            }
            return null;
        }
        if (EnumDict.class == type) {
            return dict;
        }
        if (String.class == type) {
            return dict.getValue();
        }
        return FastBeanCopier.DEFAULT_CONVERT
            .convert(dict, type, FastBeanCopier.EMPTY_CLASS_ARRAY);
    }
}

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
package org.jetlinks.community.io.excel.converter;

import lombok.AllArgsConstructor;
import org.hswebframework.reactor.excel.ExcelHeader;
import org.hswebframework.web.bean.FastBeanCopier;
import org.jetlinks.community.utils.ConverterUtils;

import java.util.List;

@AllArgsConstructor
public class ArrayConverter implements ConverterExcelOption {

    private boolean array;

    private Class<?> elementType;

    private ConverterExcelOption converter;


    @Override
    public Object convertForWrite(Object val, ExcelHeader header) {
        return String.join(",",
                           ConverterUtils.convertToList(val, v -> {
                               if (converter == null) {
                                   return String.valueOf(v);
                               }
                               return String.valueOf(converter.convertForWrite(v, header));
                           }));
    }

    @Override
    public Object convertForRead(Object cell, ExcelHeader header) {

        List<Object> list = ConverterUtils
            .convertToList(cell, val -> {
                if (converter != null) {
                    val = converter.convertForRead(val, header);
                }
                if (elementType.isInstance(val)) {
                    return val;
                }
                return FastBeanCopier.DEFAULT_CONVERT
                    .convert(val, elementType, FastBeanCopier.EMPTY_CLASS_ARRAY);
            });

        if (array) {
            return list.toArray();
        }

        return list;
    }
}

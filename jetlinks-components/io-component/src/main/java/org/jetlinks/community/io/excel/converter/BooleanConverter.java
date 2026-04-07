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
import org.hswebframework.web.i18n.LocaleUtils;
import org.jetlinks.community.io.excel.annotation.ExcelBooleanMapping;
import org.jetlinks.reactor.ql.utils.CastUtils;

public class BooleanConverter implements ConverterExcelOption {

    public static final BooleanConverter INSTANCE = new BooleanConverter(null);

    private final ExcelBooleanMapping mapping;

    public BooleanConverter(ExcelBooleanMapping mapping) {
        this.mapping = mapping;
    }

    @Override
    public Object convertForWrite(Object val, ExcelHeader header) {
        if (val == null) {
            return null;
        }

        if (mapping == null) {
            String trueText = "excel.write.true.text";
            String falseText = "excel.write.false.text";
            return val instanceof Boolean
                ? ((Boolean) val ? LocaleUtils.resolveMessage(trueText) : LocaleUtils.resolveMessage(falseText))
                : String.valueOf(val);
        }
        if (mapping.ignoreWrite()) {
            return String.valueOf(val);
        }

        return val instanceof Boolean
            ? ((Boolean) val ? LocaleUtils.resolveMessage(mapping.writeTrueText()) : LocaleUtils.resolveMessage(mapping.writeFalseText()))
            : String.valueOf(val);
    }

    @Override
    public Object convertForRead(Object val, ExcelHeader header) {
        if (val == null) {
            return null;
        }
        String text = val.toString();
        String trueVal = "excel.read.true.text";
        String falseVal = "excel.read.false.text";
        if (mapping != null) {
            trueVal = mapping.readTrueText();
            falseVal = mapping.readFalseText();
        }
        return convertForRead(text, trueVal, falseVal);
    }

    public Object convertForRead(Object val, String trueText, String falseText) {
        String text = val.toString();
        if (text.equals(LocaleUtils.resolveMessage(trueText))) {
            return true;
        } else if (text.equals(LocaleUtils.resolveMessage(falseText))) {
            return false;
        } else {
            return CastUtils.castBoolean(val);
        }
    }
}
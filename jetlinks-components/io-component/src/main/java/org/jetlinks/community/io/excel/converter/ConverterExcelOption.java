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

import org.hswebframework.reactor.excel.ExcelHeader;
import org.hswebframework.reactor.excel.ExcelOption;

/**
 * excel数据转换操作,用于自定义数据转换.
 *
 * @author zhouhao
 * @see org.jetlinks.community.io.excel.annotation.ExcelHeader#converter()
 * @since 2.1
 */
public interface ConverterExcelOption extends ExcelOption {

    Object convertForWrite(Object val, ExcelHeader header);

    Object convertForRead(Object cell, ExcelHeader header);

}

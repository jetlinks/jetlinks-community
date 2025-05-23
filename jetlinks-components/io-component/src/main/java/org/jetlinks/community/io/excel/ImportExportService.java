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
package org.jetlinks.community.io.excel;


import org.hswebframework.reactor.excel.converter.RowWrapper;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.io.InputStream;

/**
 * @author bsetfeng
 * @since 1.0
 **/
public interface ImportExportService {

    <T> Flux<RowResult<T>> doImport(Class<T> clazz, String fileUrl);


    <T> Flux<RowResult<T>> doImport(Class<T> clazz, InputStream stream);

    Mono<InputStream> getInputStream(String fileUrl);

    <T> Flux<T> readData(String fileUrl, String fileId, RowWrapper<T> wrapper);



}

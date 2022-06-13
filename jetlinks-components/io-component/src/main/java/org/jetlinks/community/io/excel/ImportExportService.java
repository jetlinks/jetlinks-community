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

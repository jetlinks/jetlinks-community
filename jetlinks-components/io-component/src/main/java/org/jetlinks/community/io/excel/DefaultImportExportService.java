package org.jetlinks.community.io.excel;

import org.jetlinks.community.io.excel.easyexcel.ExcelReadDataListener;
import org.springframework.core.io.Resource;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.io.FileInputStream;
import java.io.InputStream;

/**
 * @author bsetfeng
 * @since 1.0
 **/
@Component
public class DefaultImportExportService implements ImportExportService {

    private WebClient client;

    public DefaultImportExportService(WebClient.Builder builder) {
        client = builder.build();
    }

    public <T> Flux<RowResult<T>> doImport(Class<T> clazz, String fileUrl) {
        return getInputStream(fileUrl)
            .flatMapMany(inputStream -> ExcelReadDataListener.of(inputStream, clazz));
    }

    @Override
    public <T> Flux<RowResult<T>> doImport(Class<T> clazz, InputStream stream) {
        return ExcelReadDataListener.of(stream, clazz);
    }

    public Mono<InputStream> getInputStream(String fileUrl) {

        return Mono.defer(() -> {
            if (fileUrl.startsWith("http")) {
                return client
                    .get()
                    .uri(fileUrl)
                    .accept(MediaType.APPLICATION_OCTET_STREAM)
                    .exchange()
                    .flatMap(clientResponse -> clientResponse.bodyToMono(Resource.class))
                    .flatMap(resource -> Mono.fromCallable(resource::getInputStream));
            } else {
                return Mono.fromCallable(() -> new FileInputStream(fileUrl));
            }
        });

    }
}

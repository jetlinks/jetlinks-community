package org.jetlinks.community.protocol.local;

import lombok.AllArgsConstructor;
import lombok.Generated;
import org.jetlinks.core.spi.ServiceContext;
import org.jetlinks.community.ValueObject;
import org.jetlinks.supports.protocol.management.ProtocolSupportDefinition;
import org.jetlinks.supports.protocol.management.ProtocolSupportLoaderProvider;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.io.File;
import java.io.FileNotFoundException;

@AllArgsConstructor
@Generated
public class LocalProtocolSupportLoader implements ProtocolSupportLoaderProvider {

    private final ServiceContext serviceContext;

    @Override
    public String getProvider() {
        return "local";
    }

    @Override
    public Mono<LocalFileProtocolSupport> load(ProtocolSupportDefinition definition) {

        return Mono
            .fromCallable(() -> {
                ValueObject config = ValueObject.of(definition.getConfiguration());

                String location = config
                    .getString("location")
                    .orElseThrow(() -> new IllegalArgumentException("location cannot be null"));
                String provider = config.get("provider")
                    .map(String::valueOf)
                    .map(String::trim)
                    .orElse(null);
                File file = new File(location);
                if (!file.exists()) {
                    throw new FileNotFoundException("文件" + file.getName() + "不存在");
                }

                LocalFileProtocolSupport support = new LocalFileProtocolSupport();
                support.init(file, serviceContext, provider);
                return support;
            })
            .subscribeOn(Schedulers.boundedElastic());
    }
}

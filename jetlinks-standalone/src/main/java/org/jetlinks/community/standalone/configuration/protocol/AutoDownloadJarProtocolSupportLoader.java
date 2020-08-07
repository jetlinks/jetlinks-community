package org.jetlinks.community.standalone.configuration.protocol;

import lombok.extern.slf4j.Slf4j;
import org.hswebframework.web.bean.FastBeanCopier;
import org.jetlinks.core.ProtocolSupport;
import org.jetlinks.core.spi.ServiceContext;
import org.jetlinks.community.utils.TimeUtils;
import org.jetlinks.supports.protocol.management.ProtocolSupportDefinition;
import org.jetlinks.supports.protocol.management.jar.JarProtocolSupportLoader;
import org.jetlinks.supports.protocol.management.jar.ProtocolClassLoader;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;
import org.springframework.util.StreamUtils;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import javax.annotation.PreDestroy;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.time.Duration;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.TimeoutException;

@Component
@Slf4j
public class AutoDownloadJarProtocolSupportLoader extends JarProtocolSupportLoader {

    final WebClient webClient;

    final File tempPath;

    private final Duration loadTimeout = TimeUtils.parse(System.getProperty("jetlinks.protocol.load.timeout", "10s"));

    public AutoDownloadJarProtocolSupportLoader(WebClient.Builder builder) {
        this.webClient = builder.build();
        tempPath = new File(".temp");
        tempPath.mkdir();
    }

    @Override
    @Autowired
    public void setServiceContext(ServiceContext serviceContext) {
        super.setServiceContext(serviceContext);
    }

    @Override
    @PreDestroy
    protected void closeAll() {
        super.closeAll();
    }

    @Override
    protected void closeLoader(ProtocolClassLoader loader) {
        super.closeLoader(loader);
        for (URL url : loader.getUrls()) {
            if (new File(url.getFile()).delete()) {
                log.debug("delete old protocol:{}", url);
            }
        }
    }

    @Override
    public Mono<? extends ProtocolSupport> load(ProtocolSupportDefinition definition) {

        ProtocolSupportDefinition newDef = FastBeanCopier.copy(definition,new ProtocolSupportDefinition());

        Map<String, Object> config =newDef.getConfiguration();
        String location = Optional.ofNullable(config.get("location"))
            .map(String::valueOf).orElseThrow(() -> new IllegalArgumentException("location"));

        if (location.startsWith("http")) {
            return webClient.get()
                .uri(location)
                .exchange()
                .flatMap(clientResponse -> clientResponse.bodyToMono(Resource.class))
                .flatMap(resource -> Mono.fromCallable(resource::getInputStream))
                .flatMap(stream -> Mono.fromCallable(() -> {
                    File file = new File(tempPath, (newDef.getId() + "_" + System.currentTimeMillis()) + ".jar");
                    log.debug("write protocol file {} to {}", location, file.getAbsolutePath());
                    try (InputStream input = stream;
                         OutputStream out = new FileOutputStream(file)) {
                        StreamUtils.copy(input, out);
                    }
                    return file.getAbsolutePath();
                }))
                .subscribeOn(Schedulers.elastic())
                .doOnNext(path -> config.put("location", path))
                .then(super.load(newDef))
                .timeout(loadTimeout, Mono.error(() -> new TimeoutException("获取协议文件失败:" + location)))
                ;
        }
        return super.load(newDef);
    }
}

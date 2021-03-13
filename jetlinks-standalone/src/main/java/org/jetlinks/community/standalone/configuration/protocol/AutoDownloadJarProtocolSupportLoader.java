package org.jetlinks.community.standalone.configuration.protocol;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.codec.digest.DigestUtils;
import org.hswebframework.web.bean.FastBeanCopier;
import org.jetlinks.core.ProtocolSupport;
import org.jetlinks.core.spi.ServiceContext;
import org.jetlinks.community.utils.TimeUtils;
import org.jetlinks.supports.protocol.management.ProtocolSupportDefinition;
import org.jetlinks.supports.protocol.management.jar.JarProtocolSupportLoader;
import org.jetlinks.supports.protocol.management.jar.ProtocolClassLoader;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DataBufferUtils;
import org.springframework.stereotype.Component;
import org.springframework.util.StreamUtils;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import javax.annotation.PreDestroy;
import java.io.*;
import java.net.URL;
import java.nio.channels.AsynchronousFileChannel;
import java.nio.file.Path;
import java.time.Duration;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.TimeoutException;

import static java.nio.file.StandardOpenOption.CREATE;
import static java.nio.file.StandardOpenOption.WRITE;

@Component
@Slf4j
public class AutoDownloadJarProtocolSupportLoader extends JarProtocolSupportLoader {


    final WebClient webClient;

    final File tempPath;

    private final Duration loadTimeout = TimeUtils.parse(System.getProperty("jetlinks.protocol.load.timeout", "10s"));

    public AutoDownloadJarProtocolSupportLoader(WebClient.Builder builder) {
        this.webClient = builder.build();
        tempPath = new File(System.getProperty("jetlinks.protocol.temp.path","./data/protocols"));
        tempPath.mkdirs();
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
//        for (URL url : loader.getUrls()) {
//            if (new File(url.getFile()).delete()) {
//                log.debug("delete old protocol:{}", url);
//            }
//        }
    }

    @Override
    public Mono<? extends ProtocolSupport> load(ProtocolSupportDefinition definition) {

        ProtocolSupportDefinition newDef = FastBeanCopier.copy(definition, new ProtocolSupportDefinition());

        Map<String, Object> config = newDef.getConfiguration();
        String location = Optional
            .ofNullable(config.get("location"))
            .map(String::valueOf)
            .orElseThrow(() -> new IllegalArgumentException("configuration.location不能为空"));

        if (location.startsWith("http")) {
            String urlMd5 = DigestUtils.md5Hex(location);
            //地址没变则直接加载本地文件
            File file = new File(tempPath, (newDef.getId() + "_" + urlMd5) + ".jar");
            if (file.exists()) {
                config.put("location", file.getAbsolutePath());
                return super
                    .load(newDef)
                    .subscribeOn(Schedulers.elastic())
                    .doOnError(err -> file.delete());
            }
            return webClient
                .get()
                .uri(location)
                .retrieve()
                .bodyToFlux(DataBuffer.class)
                .as(dataStream -> {
                    log.debug("download protocol file {} to {}", location, file.getAbsolutePath());
                    return DataBufferUtils
                        .write(dataStream, file.toPath(), CREATE, WRITE)
                        .thenReturn(file.getAbsolutePath());
                })
                .subscribeOn(Schedulers.elastic())
                .doOnNext(path -> config.put("location", path))
                .then(super.load(newDef))
                .timeout(loadTimeout, Mono.error(() -> new TimeoutException("获取协议文件失败:" + location)))
                .doOnError(err -> file.delete())
                ;
        }
        return super.load(newDef);
    }
}

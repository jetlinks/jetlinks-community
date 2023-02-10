package org.jetlinks.community.protocol;

import lombok.Generated;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.codec.digest.DigestUtils;
import org.hswebframework.web.bean.FastBeanCopier;
import org.jetlinks.core.ProtocolSupport;
import org.jetlinks.core.spi.ServiceContext;
import org.jetlinks.community.io.file.FileManager;
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
import org.springframework.util.StringUtils;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import javax.annotation.PreDestroy;
import java.io.*;
import java.net.URL;
import java.nio.channels.AsynchronousFileChannel;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.TimeoutException;

import static java.nio.file.StandardOpenOption.*;

/**
 * 自动下载并缓存协议包，
 * <pre>
 *     1. 下载的协议包报错在./data/protocols目录下，可通过启动参数-Djetlinks.protocol.temp.path进行配置
 *     2. 文件名规则: 协议ID+"_"+md5(文件地址)
 *     3. 如果文件不存在则下载协议
 * </pre>
 *
 * @author zhouhao
 * @since 1.3
 */
@Slf4j
public class AutoDownloadJarProtocolSupportLoader extends JarProtocolSupportLoader {

    final WebClient webClient;

    final File tempPath;

    private final Duration loadTimeout = TimeUtils.parse(System.getProperty("jetlinks.protocol.load.timeout", "30s"));

    private final FileManager fileManager;

    public AutoDownloadJarProtocolSupportLoader(WebClient.Builder builder,
                                                FileManager fileManager) {
        this.webClient = builder.build();
        this.fileManager = fileManager;
        tempPath = new File(System.getProperty("jetlinks.protocol.temp.path", "./data/protocols"));
        tempPath.mkdirs();
    }

    @Override
    @Autowired
    @Generated
    public void setServiceContext(ServiceContext serviceContext) {
        super.setServiceContext(serviceContext);
    }

    @Override
    @PreDestroy
    @Generated
    protected void closeAll() {
        super.closeAll();
    }

    @Override
    protected void closeLoader(ProtocolClassLoader loader) {
        super.closeLoader(loader);
    }

    @Override
    public Mono<? extends ProtocolSupport> load(ProtocolSupportDefinition definition) {

        //复制新的配置信息
        ProtocolSupportDefinition newDef = FastBeanCopier.copy(definition, new ProtocolSupportDefinition());

        Map<String, Object> config = newDef.getConfiguration();
        String location = Optional
            .ofNullable(config.get("location"))
            .map(String::valueOf)
            .orElse(null);
        //远程文件则先下载再加载
        if (StringUtils.hasText(location) && location.startsWith("http")) {
            String urlMd5 = DigestUtils.md5Hex(location);
            //地址没变则直接加载本地文件
            File file = new File(tempPath, (newDef.getId() + "_" + urlMd5) + ".jar");
            if (file.exists()) {
                //设置文件地址文本地文件
                config.put("location", file.getAbsolutePath());
                return super
                    .load(newDef)
                    .subscribeOn(Schedulers.boundedElastic())
                    //加载失败则删除文件,防止文件内容错误时,一直无法加载
                    .doOnError(err -> file.delete());
            }
            return webClient
                .get()
                .uri(location)
                .retrieve()
                .bodyToFlux(DataBuffer.class)
                .as(dataStream -> {
                    log.debug("download protocol file {} to {}", location, file.getAbsolutePath());
                    //写出文件
                    return DataBufferUtils
                        .write(dataStream, file.toPath(), CREATE, WRITE)
                        .thenReturn(file.getAbsolutePath());
                })
                //使用弹性线程池来写出文件
                .subscribeOn(Schedulers.boundedElastic())
                //设置本地文件路径
                .doOnNext(path -> config.put("location", path))
                .then(super.load(newDef))
                .timeout(loadTimeout, Mono.error(() -> new TimeoutException("获取协议文件失败:" + location)))
                //失败时删除文件
                .doOnError(err -> file.delete())
                ;
        }

        //使用文件管理器获取文件
        String fileId = (String) config.getOrDefault("fileId", null);
        if (!StringUtils.hasText(fileId)) {
            return Mono.error(new IllegalArgumentException("location or fileId can not be empty"));
        }
        return loadFromFileManager(newDef.getId(), fileId)
            .flatMap(file -> {
                config.put("location", file.getAbsolutePath());
                return super
                    .load(newDef)
                    .subscribeOn(Schedulers.boundedElastic())
                    //加载失败则删除文件,防止文件内容错误时,一直无法加载
                    .doOnError(err -> file.delete());
            });

    }

    private Mono<File> loadFromFileManager(String protocolId, String fileId) {
        Path path = Paths.get(tempPath.getPath(), (protocolId + "_" + fileId) + ".jar");

        File file = path.toFile();
        if (file.exists()) {
            return Mono.just(file);
        }

        return DataBufferUtils
            .write(fileManager.read(fileId),
                   path, CREATE_NEW, TRUNCATE_EXISTING, WRITE)
            .thenReturn(file);
    }

}

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
package org.jetlinks.community.plugin.impl.jar;

import lombok.Setter;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.codec.digest.DigestUtils;
import org.hswebframework.web.exception.BusinessException;
import org.hswebframework.web.exception.I18nSupportException;
import org.hswebframework.web.id.IDGenerator;
import org.jetlinks.core.utils.ClassUtils;
import org.jetlinks.plugin.core.PluginDriver;
import org.jetlinks.community.io.file.FileInfo;
import org.jetlinks.community.io.utils.FileUtils;
import org.jetlinks.community.plugin.PluginDriverConfig;
import org.jetlinks.community.plugin.impl.PluginDriverInstallerProvider;
import org.jetlinks.community.utils.ReactorUtils;
import org.jetlinks.community.utils.TimeUtils;
import org.jetlinks.supports.protocol.validator.MethodDeniedClassVisitor;
import org.springframework.core.io.buffer.DataBufferUtils;
import org.springframework.util.StringUtils;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

import static java.nio.file.StandardOpenOption.*;

/**
 * 基于jar包的方式加载驱动.
 * <p>
 * 在{@link PluginDriverConfig#getConfiguration()}配置中设置<code>location</code>来指定jar包的文件访问地址(http).
 * <p>
 * 首次加载时,会将文件下载到本地临时目录,文件名格式为<code>{id}_{md5(location)}.jar</code>.
 * 如果文件地址变更或者文件不存在,会重新进行下载.
 * <p>
 * <h3>加载逻辑</h3>
 * 优先使用{@link ServiceLoader}来加载驱动: 通过配置<code>META-INF/services/org.jetlinks.plugin.core.PluginDriver</code>
 * 指定驱动实现类.
 * <p>
 * 如果未配置,则将扫描整个jar包中的{@link PluginDriver}的实现类并初始化.
 * </p>
 *
 * @author zhouhao
 * @since 2.0
 */
@Slf4j
public class JarPluginDriverInstallerProvider implements PluginDriverInstallerProvider {
    public static final String PROVIDER = "jar";
    private static final String TEMP_ID = "temp";
    public static final String LOCATION_KEY = "location";

    @Setter
    private Duration loadTimeout = TimeUtils.parse(System.getProperty("jetlinks.plugin.load.timeout", "1m"));

    //保存驱动文件的临时目录
    private final File tempPath = new File("./data/plugin-drivers");

    //已经加载的驱动ClassLoader
    private final Map<String, PluginClassLoader> pluginLoaders = new ConcurrentHashMap<>();

    private final MethodDeniedClassVisitor classVisitor;
    private final WebClient webClient;
    public JarPluginDriverInstallerProvider(WebClient.Builder builder) {
        this.classVisitor = MethodDeniedClassVisitor.global();
        this.webClient = builder.build();
        tempPath.mkdirs();
    }

    @Override
    public String provider() {
        return PROVIDER;
    }

    private Mono<File> loadDriverFilePath(PluginDriverConfig config) {
        String location = Optional
            .ofNullable(config.getConfiguration().get(LOCATION_KEY))
            .map(String::valueOf)
            .orElseThrow(() -> new IllegalArgumentException("location can not be null"));

        //远程文件则先下载再加载
        if (StringUtils.hasText(location) && location.startsWith("http")) {
            String urlMd5 = DigestUtils.md5Hex(location);
            //地址没变则直接加载本地文件
            File file = new File(tempPath, config.getId() + "_" + urlMd5 + ".jar");
            if (file.exists()) {
                // TODO: 2023/2/8 校验MD5
                return Mono.just(file);
            }
            return FileUtils
                .readDataBuffer(webClient,location)
                .as(dataStream -> {
                    if (log.isDebugEnabled()) {
                        log.debug("download protocol file {} to {}", location, file);
                    }
                    //写出文件
                    return DataBufferUtils
                        .write(dataStream, file.toPath(), CREATE, WRITE,TRUNCATE_EXISTING)
                        .thenReturn(file);
                })
                //使用弹性线程池来写出文件
                .subscribeOn(Schedulers.boundedElastic())
                .timeout(loadTimeout,
                         Mono.error(() -> new I18nSupportException
                             .NoStackTrace("error.load_plugin_file_timeout", location)))
                //失败时删除文件
                .doOnError(err -> file.delete())
                .thenReturn(file);
        }
        return Mono.empty();
    }

    private Mono<PluginDriver> loadFromFile(PluginDriverConfig config,
                                            File file) {

        return Mono.defer(() -> {

            PluginClassLoader classLoader = pluginLoaders.compute(config.getId(), (id, old) -> {
                if (old != null) {
                    closeClassLoader(old);
                }
                return createClassLoader(file);
            });

            PluginDriver driver = lookupDriver(classLoader);

            if (driver == null) {
                return Mono
                    .error(() -> new BusinessException
                        .NoStackTrace("error.please_upload_the_correct_plugin_package", 400)
                    );
            }
            return Mono.just(driver);
        });
    }

    protected void closeClassLoader(PluginClassLoader loader) {
        if (null == loader) {
            return;
        }
        try {
            loader.close();
        } catch (IOException ignore) {

        }
    }

    @Override
    public Mono<PluginDriver> reload(PluginDriver driver, PluginDriverConfig config) {
        ReactorUtils.dispose(driver);
        return install(config);
    }

    protected PluginDriver lookupDriver(PluginClassLoader classLoader) {

        //扫描
        PluginDriver scan = ClassUtils
            .findImplClass(PluginDriver.class,
                           "classpath:**/*.class",
                           true,
                           classLoader,
                           (loader, name, clazz) -> classVisitor.validate(name, clazz),
                           (loader, name, clazz) -> loader.loadSelfClass(name))
            .orElse(null);

        try {
            Iterator<PluginDriver> driverIterator = ServiceLoader.load(PluginDriver.class, classLoader).iterator();
            if (driverIterator.hasNext()) {
                return driverIterator.next();
            }
        } catch (Throwable ignore) {

        }
        return scan;
    }

    @SneakyThrows
    protected PluginClassLoader createClassLoader(File location) {
        return new PluginClassLoader(new URL[]{location.toURI().toURL()}, this.getClass().getClassLoader());
    }

    @Override
    public Mono<PluginDriver> install(PluginDriverConfig config) {
        return loadDriverFilePath(config)
            .flatMap(file -> loadFromFile(config, file))
            .subscribeOn(Schedulers.boundedElastic());
    }

    @Override
    public Mono<Void> uninstall(PluginDriverConfig config) {
        return Mono
            .<Void>fromCallable(() -> {
                //关闭classloader
                closeClassLoader(pluginLoaders.remove(config.getId()));
                //删除驱动文件
                Files.walk(tempPath.toPath())
                     .forEach(path -> {
                         File file = path.toFile();
                         if (file.getName().startsWith(config.getId() + "_")) {
                             file.delete();
                         }
                     });
                return null;
            })
            .subscribeOn(Schedulers.boundedElastic());
    }

    /**
     * 创建临时的插件配置
     *
     * @param fileInfo 文件信息
     * @return 插件配置
     */
    public static PluginDriverConfig tempConfig(FileInfo fileInfo) {
        PluginDriverConfig config = new PluginDriverConfig();
        config.setId(IDGenerator.RANDOM.generate() + "-" + TEMP_ID);
        config.setProvider(PROVIDER);
        config.setConfiguration(Collections.singletonMap(LOCATION_KEY, fileInfo.getAccessUrl()));
        return config;
    }
}

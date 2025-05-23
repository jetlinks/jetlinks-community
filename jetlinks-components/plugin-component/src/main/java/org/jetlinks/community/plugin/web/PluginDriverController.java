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
package org.jetlinks.community.plugin.web;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.hswebframework.web.authorization.annotation.QueryAction;
import org.hswebframework.web.authorization.annotation.Resource;
import org.hswebframework.web.authorization.annotation.SaveAction;
import org.hswebframework.web.bean.FastBeanCopier;
import org.hswebframework.web.crud.service.ReactiveCrudService;
import org.hswebframework.web.crud.web.reactive.ReactiveServiceCrudController;
import org.hswebframework.web.exception.ValidationException;
import org.hswebframework.web.i18n.LocaleUtils;
import org.hswebframework.web.validator.CreateGroup;
import org.jetlinks.core.metadata.DataType;
import org.jetlinks.core.metadata.FunctionMetadata;
import org.jetlinks.core.metadata.PropertyMetadata;
import org.jetlinks.core.metadata.types.ObjectType;
import org.jetlinks.core.utils.TypeScriptUtils;
import org.jetlinks.plugin.core.Description;
import org.jetlinks.plugin.core.PluginDriver;
import org.jetlinks.plugin.core.PluginType;
import org.jetlinks.plugin.internal.device.DeviceGatewayPluginDriver;
import org.jetlinks.plugin.internal.device.DeviceProduct;
import org.jetlinks.community.io.file.FileInfo;
import org.jetlinks.community.io.file.FileManager;
import org.jetlinks.community.io.file.FileOption;
import org.jetlinks.community.io.utils.FileUtils;
import org.jetlinks.community.plugin.PluginDriverConfig;
import org.jetlinks.community.plugin.PluginDriverInstaller;
import org.jetlinks.community.plugin.PluginDriverManager;
import org.jetlinks.community.plugin.impl.PluginDriverEntity;
import org.jetlinks.community.plugin.impl.PluginDriverService;
import org.jetlinks.community.plugin.impl.jar.JarPluginDriverInstallerProvider;
import org.jetlinks.community.web.response.ValidationResult;
import org.springframework.http.HttpStatus;
import org.springframework.http.codec.multipart.FilePart;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.*;
import java.util.function.Function;

@AllArgsConstructor
@RestController
@RequestMapping("/plugin/driver")
@Resource(id = "plugin-driver", name = "插件驱动管理")
@Tag(name = "插件驱动管理")
public class PluginDriverController implements ReactiveServiceCrudController<PluginDriverEntity, String> {
    private final PluginDriverService service;

    private final PluginDriverManager driverManager;

    private final FileManager fileManager;

    private final PluginDriverInstaller installer;

    private final Set<String> allowResourceExt = new HashSet<>(
        Arrays.asList(
            "js", "ts", "vue", "json",
            "md", "html", "css",
            "ttf", "woff", "woff2", "zip",
            "jpg", "jpeg", "png", "gif"
        )
    );

    @Override
    public ReactiveCrudService<PluginDriverEntity, String> getService() {
        return service;
    }

    @GetMapping("/types")
    @QueryAction
    public Flux<PluginTypeInfo> getPluginTypes() {
        return driverManager
            .getDrivers()
            .map(PluginDriver::getType)
            .distinct(PluginType::getId)
            .map(PluginTypeInfo::of);
    }


    @GetMapping("/{driverId}/**")
    @QueryAction
    public Mono<Void> getResource(@PathVariable String driverId,
                                  ServerWebExchange exchange) {
        String _path = exchange.getRequest().getPath().value();
        _path = _path.substring(_path.indexOf(driverId) + driverId.length() + 1);
        String path = _path;

        String ext = FileUtils.getExtension(path);

        //校验是否允许获取的文件拓展名
        if (!allowResourceExt.contains(ext)) {
            exchange
                .getResponse()
                .setStatusCode(HttpStatus.NOT_FOUND);
            return Mono.empty();
        }

        exchange.getResponse()
                .getHeaders()
                .setContentType(FileUtils.getMediaTypeByExtension(ext));

        return driverManager
            .getDriver(driverId)
            .map(driver -> exchange.getResponse().writeWith(driver.getResource(path)))
            .switchIfEmpty(Mono.fromRunnable(() -> exchange
                .getResponse()
                .setStatusCode(HttpStatus.NOT_FOUND)))
            .flatMap(Function.identity());
    }

    @GetMapping("/{driverId}/description")
    @QueryAction
    @Operation(summary = "获取插件详情")
    public Mono<Description> description(@PathVariable String driverId) {
        return driverManager
            .getDriver(driverId)
            .map(PluginDriver::getDescription);
    }

    @GetMapping("/{driverId}/products")
    @QueryAction
    @Operation(summary = "获取插件支持的产品信息")
    public Flux<DeviceProduct> deviceProduct(@PathVariable String driverId) {
        return driverManager
            .getDriver(driverId)
            .filter(driver -> driver.isWrapperFor(DeviceGatewayPluginDriver.class))
            .flatMapMany(driver -> driver
                .isWrapperFor(DeviceGatewayPluginDriver.class)
                ? driver.unwrap(DeviceGatewayPluginDriver.class).getSupportProducts()
                : Flux.empty());
    }

    @PostMapping("/upload")
    @SaveAction
    @Operation(summary = "上传文件，返回插件信息")
    public Mono<PluginDriverUploadInfo> upload(@RequestPart("file") Mono<FilePart> partMono) {
        return partMono
            .flatMap(fileManager::saveFile)
            .flatMap(fileInfo -> {
                PluginDriverUploadInfo info = new PluginDriverUploadInfo().with(fileInfo);
                PluginDriverConfig config = JarPluginDriverInstallerProvider.tempConfig(fileInfo);
                return installer
                    .install(config)
                    .doOnNext(info::with)
                    .then(installer.uninstall(config))
                    .thenReturn(info);
            });
    }

    @PostMapping("/convert")
    @SaveAction
    @Operation(summary = "获取插件详情信息")
    public Mono<PluginDriverUploadInfo> convertToDetail(@RequestBody Mono<PluginDriverEntity> pluginDriver) {
        return pluginDriver
            .map(PluginDriverEntity::toConfig)
            .doOnNext(config -> config.setId("_debug"))
            .flatMap(def -> {
                PluginDriverUploadInfo uploadInfo = new PluginDriverUploadInfo();
                return installer
                    .install(def)
                    .map(uploadInfo::with)
                    .flatMap(info -> installer
                        .uninstall(def)
                        .thenReturn(info));
            });
    }

    @GetMapping("/id/_validate")
    @QueryAction
    @Operation(summary = "验证插件ID是否合法")
    public Mono<ValidationResult> idValidate(@RequestParam @Parameter(description = "插件ID") String id) {
        PluginDriverEntity entity = new PluginDriverEntity();
        entity.setId(id);
        entity.tryValidate("id", CreateGroup.class);

        return service
            .findById(id)
            .flatMap(ignore -> LocaleUtils.resolveMessageReactive("error.plugin_driver_id_already_exists"))
            .map(ValidationResult::error)
            .defaultIfEmpty(ValidationResult.success())
            .onErrorResume(ValidationException.class, e -> Mono.just(e.getI18nCode())
                                                               .map(ValidationResult::error));
    }

    @Getter
    @Setter
    @AllArgsConstructor
    @NoArgsConstructor
    public static class PluginTypeInfo {
        private String id;
        private String name;

        public String getValue() {
            return id;
        }

        public String getText() {
            return name;
        }

        public static PluginTypeInfo of(PluginType type) {
            return new PluginTypeInfo(type.getId(), type.getName());
        }
    }

    @Setter
    @Getter
    public static class PluginDriverUploadInfo {

        @Schema(description = "插件ID")
        private String id;

        @Schema(description = "插件名称")
        private String name;

        @Schema(description = "插件说明")
        private String description;

        @Schema(description = "插件版本")
        private String version;

        @Schema(description = "插件类型")
        private PluginTypeInfo type;

        @Schema(description = "文件访问地址")
        private String accessUrl;

        @Schema(description = "文件名称")
        private String filename;

        @Schema(description = "文件后缀")
        private String extension;

        @Schema(description = "文件长度")
        private long length;

        @Schema(description = "md5")
        private String md5;

        @Schema(description = "sha256")
        private String sha256;

        @Schema(description = "创建时间")
        private long createTime;

        @Schema(description = "创建者ID")
        private String creatorId;

        @Schema(description = "文件配置")
        private FileOption[] options;

        @Schema(description = "其他信息")
        private Map<String, Object> others;

        public PluginDriverUploadInfo with(FileInfo fileInfo) {
            FastBeanCopier.copy(fileInfo, this, "id", "name");
            setFilename(fileInfo.getName());
            return this;
        }

        public PluginDriverUploadInfo with(PluginDriver pluginDriver) {
            setId(pluginDriver.getDescription().getId());
            setName(pluginDriver.getDescription().getName());
            setDescription(pluginDriver.getDescription().getDescription());
            setVersion(pluginDriver.getDescription().getVersion().toString());
            setType(PluginTypeInfo.of(pluginDriver.getType()));
            setOthers(pluginDriver.getDescription().getOthers());
            return this;
        }


    }

}

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
package org.jetlinks.community.io.file;

import io.netty.buffer.ByteBufAllocator;
import org.apache.commons.lang3.StringUtils;
import org.hswebframework.web.bean.FastBeanCopier;
import org.jetlinks.community.command.CommandSupportManagerProvider;
import org.jetlinks.community.io.utils.FileUtils;
import org.jetlinks.core.command.AbstractCommandSupport;
import org.jetlinks.core.command.CommandHandler;
import org.jetlinks.core.command.CommandSupport;
import org.jetlinks.core.command.CommandUtils;
import org.jetlinks.core.metadata.SimpleFunctionMetadata;
import org.jetlinks.sdk.server.SdkServices;
import org.jetlinks.sdk.server.commons.cmd.DeleteByIdCommand;
import org.jetlinks.sdk.server.file.DownloadFileCommand;
import org.jetlinks.sdk.server.file.FileInfo;
import org.jetlinks.sdk.server.file.StreamUploadFileCommand;
import org.jetlinks.sdk.server.file.UploadFileCommand;
import org.jetlinks.sdk.server.utils.ConverterUtils;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.NettyDataBufferFactory;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Map;

/**
 * 提供对文件服务的相关命令支持，用于在其他组件中基于命令模式调用文件服务.
 *
 * @author zhouhao
 * @see org.jetlinks.community.command.CommandSupportManagerProvider
 * @see SdkServices#fileService
 * @see UploadFileCommand
 * @since 2.2
 */
public class FileCommandSupportManager extends AbstractCommandSupport implements CommandSupportManagerProvider {
    private final FileManager fileManager;

    private final static NettyDataBufferFactory factory = new NettyDataBufferFactory(ByteBufAllocator.DEFAULT);

    public FileCommandSupportManager(FileManager fileManager) {
        this(WebClient.builder(), fileManager);
    }

    public FileCommandSupportManager(WebClient.Builder builder, FileManager fileManager) {
        this.fileManager = fileManager;
        WebClient client = builder.build();
        {
            SimpleFunctionMetadata metadata = new SimpleFunctionMetadata();
            metadata.setId(CommandUtils.getCommandIdByType(UploadFileCommand.class));
            metadata.setName("上传文件");
            registerHandler(UploadFileCommand.class,
                            CommandHandler
                                .of(metadata,
                                    (cmd, that) -> doUpload(cmd),
                                    UploadFileCommand::new));

        }

        {
            SimpleFunctionMetadata metadata = new SimpleFunctionMetadata();
            metadata.setId(CommandUtils.getCommandIdByType(StreamUploadFileCommand.class));
            metadata.setName("流式上传文件");
            registerHandler(StreamUploadFileCommand.class,
                            CommandHandler
                                .of(metadata,
                                    (cmd, that) -> doUpload(cmd),
                                    StreamUploadFileCommand::new));
        }

        //下载文件内容
        {
            registerHandler(DownloadFileCommand.class,
                            CommandHandler.of(
                                DownloadFileCommand.metadata(),
                                (cmd, that) -> FileUtils
                                    .readDataBuffer(client,cmd.getUrl())
                                    .map(ConverterUtils::convertNettyBuffer),
                                DownloadFileCommand::new
                            ));
        }

        //删除文件
        registerHandler(
            DeleteByIdCommand
                .<Mono<Void>>createHandler(metadata -> {
                                           },
                                           cmd -> {
                                               List<String> idList = cmd.getIdList(String::valueOf);
                                               if (idList.isEmpty()) {
                                                   return Mono.empty();
                                               }
                                               return Flux
                                                   .fromIterable(idList)
                                                   .flatMap(fileManager::delete)
                                                   .then();
                                           })
        );


    }

    @Override
    public String getProvider() {
        return SdkServices.fileService;
    }

    public Flux<FileInfo> doUpload(StreamUploadFileCommand command) {
        String fileId = command.getFileId();
        if (StringUtils.isNotBlank(fileId)) {
            return fileManager
                .saveFile(fileId, command.stream().map(factory::wrap))
                .map(file -> FastBeanCopier.copy(file, new FileInfo()))
                .flux();
        }
        String name = command.getFileName();
        if (name == null) {
            name = "undefined.bin";
        }
        return fileManager
            .saveFile(name, command.stream().map(factory::wrap))
            .map(file -> FastBeanCopier.copy(file, new FileInfo()))
            .flux();
    }

    public Mono<FileInfo> doUpload(UploadFileCommand command) {
        String fileId = command.getFileId();
        if (StringUtils.isNotBlank(fileId)) {
            if (command.isSharding()) {
                return fileManager
                    .saveFileById(fileId,
                                  fileId.length(),
                                  command.getOffset(),
                                  Flux.just(factory.wrap(command.getContent())))
                    .map(file -> FastBeanCopier.copy(file, new FileInfo()));
            } else {
                return fileManager
                    .saveFileById(fileId, Flux.just(factory.wrap(command.getContent())))
                    .map(file -> FastBeanCopier.copy(file, new FileInfo()));
            }
        }
        if (command.isSharding()) {
            return fileManager
                .saveFile(command.getSessionId(),
                          command.getFileName(),
                          command.getContentLength(),
                          command.getOffset(),
                          Flux.just(factory.wrap(command.getContent())))
                .map(file -> FastBeanCopier.copy(file, new FileInfo()));
        } else {
            return fileManager
                .saveFile(command.getFileName(), Flux.just(factory.wrap(command.getContent())))
                .map(file -> FastBeanCopier.copy(file, new FileInfo()));
        }
    }


    @Override
    public Mono<? extends CommandSupport> getCommandSupport(String id, Map<String, Object> options) {

        return Mono.just(this);
    }
}

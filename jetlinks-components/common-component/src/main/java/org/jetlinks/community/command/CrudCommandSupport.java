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
package org.jetlinks.community.command;

import lombok.SneakyThrows;
import org.hswebframework.web.api.crud.entity.EntityFactoryHolder;
import org.hswebframework.web.authorization.Authentication;
import org.hswebframework.web.authorization.Permission;
import org.hswebframework.web.authorization.exception.AccessDenyException;
import org.hswebframework.web.bean.FastBeanCopier;
import org.hswebframework.web.crud.service.ReactiveCrudService;
import org.jetlinks.core.command.AbstractCommandSupport;
import org.jetlinks.core.command.Command;
import org.jetlinks.core.metadata.FunctionMetadata;
import org.jetlinks.core.metadata.SimplePropertyMetadata;
import org.jetlinks.core.metadata.types.ArrayType;
import org.jetlinks.core.metadata.types.IntType;
import org.jetlinks.core.metadata.types.ObjectType;
import org.jetlinks.core.metadata.types.StringType;
import org.jetlinks.core.utils.Reactors;
import org.jetlinks.sdk.server.commons.cmd.*;
import org.jetlinks.supports.official.DeviceMetadataParser;
import org.springframework.core.ResolvableType;
import reactor.bool.BooleanUtils;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * 通用增删改查命令支持,基于{@link ReactiveCrudService}来实现增删改查相关命令
 *
 * @param <T> 实体类型
 * @author zhouhao
 * @see QueryByIdCommand
 * @see QueryPagerCommand
 * @see QueryListCommand
 * @see CountCommand
 * @see SaveCommand
 * @see AddCommand
 * @see UpdateCommand
 * @see DeleteCommand
 * @see DeleteByIdCommand
 * @since 2.2
 */
public class CrudCommandSupport<T> extends AbstractCommandSupport {

    final ReactiveCrudService<T, String> service;
    final ResolvableType _entityType;

    public CrudCommandSupport(ReactiveCrudService<T, String> service) {
        this(service, ResolvableType
            .forClass(ReactiveCrudService.class, service.getClass())
            .getGeneric(0));
    }

    public CrudCommandSupport(ReactiveCrudService<T, String> service, ResolvableType _entityType) {
        this.service = service;
        this._entityType = _entityType;

        registerQueries();
        registerSaves();
        registerDelete();
    }

    @Override
    public Flux<FunctionMetadata> getCommandMetadata() {
        return super
            .getCommandMetadata()
            .filterWhen(func -> commandIsSupported(func.getId()));
    }

    @Override
    public Mono<Boolean> commandIsSupported(String commandId) {

        return BooleanUtils.and(
            super.commandIsSupported(commandId),
            hasPermission(getPermissionId(), getAction(commandId))
        );
    }

    @Override
    public Mono<FunctionMetadata> getCommandMetadata(String commandId) {
        return super
            .getCommandMetadata(commandId)
            .filterWhen(func -> commandIsSupported(func.getId()));
    }

    @Override
    public Mono<FunctionMetadata> getCommandMetadata(Command<?> command) {
        return super
            .getCommandMetadata(command)
            .filterWhen(func -> commandIsSupported(func.getId()));
    }

    @Override
    public Mono<FunctionMetadata> getCommandMetadata(@Nonnull String commandId,
                                                     @Nullable Map<String, Object> parameters) {
        return super
            .getCommandMetadata(commandId, parameters)
            .filterWhen(func -> commandIsSupported(func.getId()));
    }

    @SneakyThrows
    @SuppressWarnings("all")
    private T newInstance0() {
        return (T) _entityType.toClass().getConstructor().newInstance();
    }

    protected T newInstance() {
        @SuppressWarnings("all")
        Class<T> clazz = (Class<T>) _entityType.toClass();
        return EntityFactoryHolder
            .newInstance(clazz,
                         this::newInstance0);
    }

    protected ResolvableType getResolvableType() {
        return _entityType;
    }

    protected ObjectType createEntityType() {
        return (ObjectType) DeviceMetadataParser.withType(_entityType);
    }

    protected String getPermissionId() {
        return null;
    }

    protected Mono<Void> assetPermission(String action) {
        return assetPermission(getPermissionId(), action);
    }

    protected Mono<Boolean> hasPermission(String permissionId, String action) {
        if (permissionId == null) {
            return Reactors.ALWAYS_TRUE;
        }
        return Authentication
            .currentReactive()
            .map(auth -> auth.hasPermission(permissionId, action))
            .defaultIfEmpty(true);
    }

    protected Mono<Void> assetPermission(String permissionId, String action) {
        if (permissionId == null) {
            return Mono.empty();
        }
        return Authentication
            .currentReactive()
            .flatMap(
                auth -> auth.hasPermission(permissionId, action)
                    ? Mono.empty()
                    : Mono.error(new AccessDenyException.NoStackTrace(permissionId, Collections.singleton(action))));
    }

    protected String getAction(String commandId) {
        if (commandId.startsWith("Delete")) {
            return Permission.ACTION_DELETE;
        }
        if (commandId.startsWith("Update") ||
            commandId.startsWith("Save") ||
            commandId.startsWith("Add") ||
            commandId.startsWith("Disable") ||
            commandId.startsWith("Enable")) {
            return Permission.ACTION_SAVE;
        }
        return Permission.ACTION_QUERY;
    }

    protected void registerQueries() {
        //根据id查询
        registerHandler(
            QueryByIdCommand
                .<T>createHandler(
                    metadata -> {
                        metadata
                            .setInputs(Collections.singletonList(
                                SimplePropertyMetadata
                                    .of("id", "id", new ArrayType()
                                        .elementType(StringType.GLOBAL))
                            ));
                        metadata.setOutput(createEntityType());
                    },
                    cmd -> assetPermission(Permission.ACTION_QUERY)
                        .then(service.findById(cmd.getId())),
                    _entityType)
        );

        //分页查询
        registerHandler(
            QueryPagerCommand
                .<T>createHandler(
                    metadata -> metadata.setOutput(
                        QueryPagerCommand
                            .createOutputType(createEntityType().getProperties())),
                    cmd -> assetPermission(Permission.ACTION_QUERY)
                        .then(service.queryPager(cmd.asQueryParam())),
                    _entityType)
        );
        //查询列表
        registerHandler(
            QueryListCommand
                .<T>createHandler(
                    metadata -> metadata.setOutput(createEntityType()),
                    cmd -> assetPermission(Permission.ACTION_QUERY)
                        .thenMany(service.query(cmd.asQueryParam())),
                    _entityType)
        );
        //查询数量
        registerHandler(
            CountCommand
                .createHandler(
                    metadata -> metadata.setOutput(new ObjectType()
                                                       .addProperty("total", "总数", IntType.GLOBAL)),
                    cmd -> assetPermission(Permission.ACTION_QUERY)
                        .then(service.count(cmd.asQueryParam())))
        );
        //todo 聚合查询?

    }

    protected void registerSaves() {
        //批量保存
        registerHandler(
            SaveCommand.createHandler(
                metadata -> {
                    metadata
                        .setInputs(Collections.singletonList(
                            SimplePropertyMetadata.of("data", "数据列表", new ArrayType().elementType(createEntityType()))
                        ));
                    metadata.setOutput(createEntityType());
                },
                cmd -> {
                    List<T> list = cmd.dataList((data) -> FastBeanCopier.copy(data, newInstance()));
                    return assetPermission(Permission.ACTION_SAVE)
                        .then(service.save(list))
                        .thenMany(Flux.fromIterable(list));
                },
                _entityType)
        );
        //新增
        registerHandler(
            AddCommand
                .<T>createHandler(
                    metadata -> {
                        metadata
                            .setInputs(Collections.singletonList(
                                SimplePropertyMetadata.of("data", "数据列表", new ArrayType().elementType(createEntityType()))
                            ));
                        metadata.setOutput(createEntityType());
                    },
                    cmd -> Flux
                        .fromIterable(cmd.dataList((data) -> FastBeanCopier.copy(data, newInstance())))
                        .as(flux -> assetPermission(Permission.ACTION_SAVE)
                            .then(service.insert(flux))
                            .thenMany(flux)))
        );
        //修改
        registerHandler(
            UpdateCommand
                .<T>createHandler(
                    metadata -> {
                        metadata.setInputs(
                            Arrays.asList(
                                SimplePropertyMetadata.of("data", "数据", createEntityType()),
                                QueryCommand.getTermsMetadata()
                            ));
                        metadata.setOutput(IntType.GLOBAL);
                    },
                    cmd -> this
                        .assetPermission(Permission.ACTION_SAVE)
                        .then(cmd
                                  .applyUpdate(service.createUpdate(), map -> FastBeanCopier.copy(map, newInstance()))
                                  .execute()))
        );
    }

    protected void registerDelete() {

        //删除
        registerHandler(
            DeleteCommand.createHandler(
                metadata -> {
                    metadata.setInputs(Collections.singletonList(
                        SimplePropertyMetadata.of("terms", "删除条件", QueryCommand.getTermsDataType())));
                    metadata.setOutput(IntType.GLOBAL);
                },
                cmd -> this
                    .assetPermission(Permission.ACTION_DELETE)
                    .then(cmd.applyDelete(service.createDelete()).execute()))
        );
        //根据id移除
        registerHandler(
            DeleteByIdCommand
                .<Mono<Void>>createHandler(
                    metadata -> {
                    },
                    cmd -> this
                        .assetPermission(Permission.ACTION_DELETE)
                        .then(service.deleteById(cmd.getId()).then()))
        );
    }

}

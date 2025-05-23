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

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hswebframework.web.bean.FastBeanCopier;
import org.jetlinks.core.command.CommandSupport;
import org.jetlinks.core.utils.SerializeUtils;
import org.jetlinks.community.spi.Provider;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.io.Externalizable;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.util.Map;

/**
 * 命令支持提供者,用于针对多个基于命令模式的可选模块依赖时的解耦.
 * <p>
 *
 * @author zhouhao
 * @see org.jetlinks.sdk.server.SdkServices
 * @see InternalSdkServices
 * @since 2.1
 */
public interface CommandSupportManagerProvider {
    /**
     * 所有支持的提供商
     */
    Provider<CommandSupportManagerProvider> supports = Provider.create(CommandSupportManagerProvider.class);

    /**
     * 命令服务提供商标识
     *
     * @return 唯一标识
     */
    String getProvider();

    /**
     * 获取命令支持,不同的命令管理支持多种命令支持,可能通过id进行区分,具体规则由对应服务实 现
     *
     * @param id      命令ID标识
     * @param options 拓展配置
     * @return CommandSupport
     * @see CommandSupportManagerProviders#getCommandSupport(String, Map)
     */
    Mono<? extends CommandSupport> getCommandSupport(String id, Map<String, Object> options);

    /**
     * 获取所有支持的信息
     *
     * @return id
     */
    default Flux<CommandSupportInfo> getSupportInfo() {
        return Flux.empty();
    }

    @Getter
    @NoArgsConstructor
    @AllArgsConstructor(staticName = "of")
    @Setter
    class CommandSupportInfo implements Externalizable {
        private String id;
        private String name;
        private String description;

        public CommandSupportInfo copy() {
            return FastBeanCopier.copy(this, new CommandSupportInfo());
        }

        /**
         * @param serviceId serviceId
         * @return this
         * @see CommandSupportManagerProviders#getCommandSupport(String)
         */
        public CommandSupportInfo appendService(String serviceId) {
            if (this.id == null) {
                this.id = serviceId;
            } else {
                this.id = serviceId + ":" + id;
            }
            return this;
        }

        @Override
        public void writeExternal(ObjectOutput out) throws IOException {
            SerializeUtils.writeNullableUTF(id, out);
            SerializeUtils.writeNullableUTF(name, out);
            SerializeUtils.writeNullableUTF(description, out);
        }

        @Override
        public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException {
            id = SerializeUtils.readNullableUTF(in);
            name = SerializeUtils.readNullableUTF(in);
            description = SerializeUtils.readNullableUTF(in);
        }
    }
}

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
package org.jetlinks.community.notify.email.embedded;

import org.hswebframework.web.i18n.LocaleUtils;
import org.jetlinks.core.metadata.ConfigMetadata;
import org.jetlinks.core.metadata.DefaultConfigMetadata;
import org.jetlinks.core.metadata.SimplePropertyMetadata;
import org.jetlinks.core.metadata.types.*;
import org.jetlinks.community.io.file.FileManager;
import org.jetlinks.community.notify.*;
import org.jetlinks.community.notify.email.EmailProvider;
import org.jetlinks.community.notify.template.TemplateManager;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import javax.annotation.Nonnull;

import static org.jetlinks.community.ConfigMetadataConstants.*;

@Component
public class DefaultEmailNotifierProvider implements NotifierProvider {

    private final TemplateManager templateManager;

    private final FileManager fileManager;

    private final WebClient.Builder builder;

    public DefaultEmailNotifierProvider(TemplateManager templateManager,
                                        FileManager fileManager,
                                        WebClient.Builder builder) {
        this.templateManager = templateManager;
        this.fileManager = fileManager;
        this.builder = builder;
    }

    @Nonnull
    @Override
    public NotifyType getType() {
        return DefaultNotifyType.email;
    }

    @Nonnull
    @Override
    public Provider getProvider() {
        return EmailProvider.embedded;
    }

    public static final DefaultConfigMetadata templateConfig;

    public static final DefaultConfigMetadata notifierConfig;

    static {
        {
            SimplePropertyMetadata name = new SimplePropertyMetadata();
            name.setId("name");
            name.setName("文件名");
            name.setValueType(new StringType());

            SimplePropertyMetadata location = new SimplePropertyMetadata();
            location.setId("location");
            location.setName("文件地址");
            location.setValueType(new FileType()
                                      .bodyType(FileType.BodyType.url)
                                      .expand(allowInput.value(true)));

            templateConfig = new DefaultConfigMetadata("邮件模版", "")
                .add("subject", "标题", "标题,可使用变量", new StringType().expand(maxLength.value(255L)))
                .add("text", "内容", "", new StringType().expand(maxLength.value(5120L), isRichText.value(true)))
                .add("sendTo", "收件人", "", new ArrayType().elementType(new StringType()))
                .add("attachments", "附件列表", "", new ArrayType()
                    .elementType(new ObjectType()
                                     .addPropertyMetadata(name)
                                     .addPropertyMetadata(location)));
        }

        {
            SimplePropertyMetadata name = new SimplePropertyMetadata();
            name.setId("name");
            name.setName("配置名称");
            name.setValueType(new StringType());

            SimplePropertyMetadata value = new SimplePropertyMetadata();
            value.setId("value");
            value.setName("配置值");
            value.setValueType(new StringType());

            SimplePropertyMetadata description = new SimplePropertyMetadata();
            description.setId("description");
            description.setName("说明");
            description.setValueType(new StringType());

            notifierConfig = new DefaultConfigMetadata("邮件配置", "")
                .add("host", "服务器地址", "例如: pop3.qq.com", new StringType().expand(maxLength.value(255L)))
                .add("port", "端口", "", new IntType().min(0).max(65536))
                .add("sender", "发件人", "默认和用户名相同", new StringType())
                .add("username", "用户名", "", new StringType())
                .add("password", "密码", "", new PasswordType())
                .add("properties", "其他配置", "", new ArrayType()
                    .elementType(new ObjectType()
                                     .addPropertyMetadata(name)
                                     .addPropertyMetadata(value)
                                     .addPropertyMetadata(description)));
        }
    }

    @Override
    public ConfigMetadata getNotifierConfigMetadata() {
        return notifierConfig;
    }

    @Nonnull
    @Override
    public Mono<DefaultEmailNotifier> createNotifier(@Nonnull NotifierProperties properties) {
        return Mono.fromSupplier(() -> new DefaultEmailNotifier(properties, templateManager, fileManager, builder))
            .as(LocaleUtils::transform);
    }

}

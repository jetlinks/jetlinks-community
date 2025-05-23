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
package org.jetlinks.community.plugin.impl;

import org.jetlinks.plugin.core.PluginDriver;
import org.jetlinks.community.plugin.PluginDriverListener;
import org.springframework.context.MessageSource;
import org.springframework.context.MessageSourceResolvable;
import org.springframework.context.NoSuchMessageException;
import org.springframework.context.support.ResourceBundleMessageSource;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.util.ObjectUtils;
import org.springframework.util.StringUtils;
import reactor.core.publisher.Mono;

import javax.annotation.Nonnull;
import java.io.IOException;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 插件采集器-国际化管理.
 *
 * @author zhangji 2024/10/26
 * @since 2.3
 */
public class PluginI18nMessageSource implements MessageSource, PluginDriverListener {

    public static final String PATTERN = "classpath:i18n/**";

    private final Map<String, MessageSource> messageSources = new ConcurrentHashMap<>();

    public void addMessageSources(Map<String, MessageSource> source) {
        messageSources.putAll(source);
    }

    public void addMessageSource(String provider, MessageSource source) {
        messageSources.put(provider, source);
    }

    public void removeMessageSource(String provider) {
        messageSources.remove(provider);
    }

    @Override
    public String getMessage(@Nonnull String code, Object[] args, String defaultMessage, @Nonnull Locale locale) {
        for (MessageSource messageSource : messageSources.values()) {
            String result = messageSource.getMessage(code, args, null, locale);
            if (StringUtils.hasText(result)) {
                return result;
            }
        }
        return defaultMessage;
    }

    @Override
    @Nonnull
    public String getMessage(@Nonnull String code, Object[] args, @Nonnull Locale locale) throws NoSuchMessageException {
        for (MessageSource messageSource : messageSources.values()) {
            try {
                String result = messageSource.getMessage(code, args, locale);
                if (StringUtils.hasText(result)) {
                    return result;
                }
            } catch (NoSuchMessageException ignore) {

            }
        }
        throw new NoSuchMessageException(code, locale);
    }

    @Override
    @Nonnull
    public String getMessage(@Nonnull MessageSourceResolvable resolvable, @Nonnull Locale locale) throws NoSuchMessageException {
        for (MessageSource messageSource : messageSources.values()) {
            try {
                String result = messageSource.getMessage(resolvable, locale);
                if (StringUtils.hasText(result)) {
                    return result;
                }
            } catch (NoSuchMessageException ignore) {

            }
        }
        String[] codes = resolvable.getCodes();
        throw new NoSuchMessageException(!ObjectUtils.isEmpty(codes) ? codes[codes.length - 1] : "", locale);
    }

    @Override
    public Mono<Void> onInstall(String driverId, PluginDriver driver) {
        ClassLoader classLoader = driver.unwrap(PluginDriver.class).getClass().getClassLoader();
        try {
            addMessageSource(
                driverId,
                getMessageSource(classLoader)
            );
        } catch (IOException ignore) {

        }

        return Mono.empty();
    }

    public MessageSource getMessageSource(ClassLoader classLoader) throws IOException {
        PathMatchingResourcePatternResolver resolver = new PathMatchingResourcePatternResolver(classLoader);
        Resource[] resources = resolver.getResources(PATTERN);
        ResourceBundleMessageSource messageSource = new ResourceBundleMessageSource();
        messageSource.setDefaultEncoding("UTF-8");
        messageSource.setBundleClassLoader(classLoader);

        for (Resource resource : resources) {
            String path = resource.getURL().getPath();
            if (StringUtils.hasText(path) && (path.endsWith(".properties") || path.endsWith(".xml"))) {
                path = path.substring(path.lastIndexOf("i18n"));
                String[] split = path.split("[/|\\\\]");
                String name = split[split.length - 1];
                name = name.contains("_") ? name.substring(0, name.indexOf("_")) : name;
                split[split.length - 1] = name;

                messageSource.addBasenames(String.join("/", split));
            }
        }
        return messageSource;
    }

    @Override
    public Mono<Void> onReload(String driverId, PluginDriver oldDriver, PluginDriver driver) {
        removeMessageSource(driverId);
        return onInstall(driverId, driver);
    }

    @Override
    public Mono<Void> onUninstall(String driverId, PluginDriver driver) {
        removeMessageSource(driverId);
        return Mono.empty();
    }
}

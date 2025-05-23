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
package org.jetlinks.community.io.file.service;

import org.apache.commons.lang3.StringUtils;
import org.springframework.core.Ordered;
import reactor.util.context.Context;
import reactor.util.context.ContextView;

import java.util.Comparator;

/**
 * @author gyl
 * @since 2.3
 */
public class FileServiceHelper {

    private static String first;

    /**
     * 获取优先级最高的文件服务类型
     *
     * @return 文件服务类型
     */
    public static String getHighOrderServiceType() {
        if (StringUtils.isNotBlank(first)) {
            return first;
        }
        synchronized (FileServiceHelper.class) {
            first = FileServiceProvider
                .providers
                .getAll()
                .stream()
                .min(Comparator.comparingInt(Ordered::getOrder))
                .map(FileServiceProvider::getType)
                .orElse(null);
        }
        return first;
    }


    /**
     * 从上下文获取指定文件服务类型，未指定则使用默认类型
     *
     * @param ctx 上下文
     * @return 文件服务类型
     */
    public static String getFileService(ContextView ctx, String defaultService) {
        String contextType = (String) ctx.getOrEmpty(FileServiceHelper.class).orElse(null);
        if (StringUtils.isBlank(contextType)) {
            return defaultService;
        }
        return contextType;
    }

    /**
     * 设置指定文件服务类型到上下文
     *
     * @param type 文件服务类型{@link FileServiceProvider#getType()}
     * @return 填充的上下文
     */
    public static ContextView setFileService(String type) {
        return Context.of(FileServiceHelper.class, type);
    }

}

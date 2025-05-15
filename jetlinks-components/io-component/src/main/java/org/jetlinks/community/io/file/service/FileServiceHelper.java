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

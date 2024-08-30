package org.jetlinks.community.dictionary;

import org.hswebframework.web.dict.EnumDict;

import javax.annotation.Nonnull;
import java.util.List;
import java.util.Optional;

/**
 * 数据字典管理器,用于获取数据字典的枚举值
 *
 * @author zhouhao
 * @since 2.1
 */
public interface DictionaryManager {

    /**
     * 获取字典的所有选项
     *
     * @param dictId 字典ID
     * @return 字典值
     */
    @Nonnull
    List<EnumDict<?>> getItems(@Nonnull String dictId);

    /**
     * 获取字段选项
     *
     * @param dictId 字典ID
     * @param itemId 选项ID
     * @return 选项值
     */
    @Nonnull
    Optional<EnumDict<?>> getItem(@Nonnull String dictId,
                                  @Nonnull String itemId);





}

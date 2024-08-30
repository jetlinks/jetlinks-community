package org.jetlinks.community.dictionary;

import org.hswebframework.web.dict.EnumDict;

import javax.annotation.Nonnull;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * 动态数据字典工具类
 *
 * @author zhouhao
 * @since 2.1
 */
public class Dictionaries {

    static DictionaryManager HOLDER = null;

    static void setup(DictionaryManager manager) {
        HOLDER = manager;
    }

    /**
     * 获取字典的所有选型
     *
     * @param dictId 字典ID
     * @return 字典值
     */
    @Nonnull
    public static List<EnumDict<?>> getItems(@Nonnull String dictId) {
        return HOLDER == null ? Collections.emptyList() : HOLDER.getItems(dictId);
    }

    /**
     * 根据掩码获取枚举选项,通常用于多选时获取选项.
     *
     * @param dictId 枚举ID
     * @param mask   掩码
     * @return 选项
     * @see Dictionaries#toMask(Collection)
     */
    @Nonnull
    public static List<EnumDict<?>> getItems(@Nonnull String dictId, long mask) {
        if (HOLDER == null) {
            return Collections.emptyList();
        }
        return HOLDER
            .getItems(dictId)
            .stream()
            .filter(item -> item.in(mask))
            .collect(Collectors.toList());
    }

    /**
     * 查找枚举选项
     *
     * @param dictId 枚举ID
     * @param value  选项值
     * @return 选项
     */
    public static Optional<EnumDict<?>> findItem(@Nonnull String dictId, Object value) {
        return getItems(dictId)
            .stream()
            .filter(item -> item.eq(value))
            .findFirst();
    }

    /**
     * 获取字段选型
     *
     * @param dictId 字典ID
     * @param itemId 选项ID
     * @return 选项值
     */
    @Nonnull
    public static Optional<EnumDict<?>> getItem(@Nonnull String dictId,
                                                @Nonnull String itemId) {
        return HOLDER == null ? Optional.empty() : HOLDER.getItem(dictId, itemId);
    }


    public static long toMask(Collection<EnumDict<?>> items) {
        long value = 0L;
        for (EnumDict<?> t1 : items) {
            value |= t1.getMask();
        }
        return value;
    }


}

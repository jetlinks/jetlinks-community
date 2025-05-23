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
package org.jetlinks.community.utils;

import io.micrometer.core.instrument.Tags;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufUtil;
import io.netty.buffer.Unpooled;
import io.netty.util.CharsetUtil;
import lombok.SneakyThrows;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.ComparatorUtils;
import org.apache.commons.collections4.MapUtils;
import org.hswebframework.ezorm.core.param.Sort;
import org.hswebframework.ezorm.core.param.Term;
import org.hswebframework.web.bean.FastBeanCopier;
import org.jetlinks.core.utils.StringBuilderUtils;
import org.jetlinks.reactor.ql.utils.CompareUtils;
import org.springframework.util.ConcurrentReferenceHashMap;
import org.springframework.util.StringUtils;
import reactor.core.publisher.Flux;

import java.nio.charset.Charset;
import java.nio.charset.CharsetEncoder;
import java.util.*;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class ConverterUtils {
    private static final Pattern HEX_PATTERN = Pattern.compile("^[0-9a-fA-F]+$");
    /**
     * 尝试转换值为集合,如果不是集合格式则直接返回该值
     *
     * @param value     值
     * @param converter 转换器,用户转换单个结果
     * @return 转换结果
     */
    public static Object tryConvertToList(Object value, Function<Object, Object> converter) {
        List<Object> list = convertToList(value, converter);
        if (list.size() == 1) {
            return converter.apply(list.get(0));
        }
        return list;
    }

    /**
     * 转换参数为指定类型的List
     *
     * @param value     参数
     * @param converter 类型转换器
     * @param <T>       List中元素类型
     * @return 转换后的List
     */
    public static <T> List<T> convertToList(Object value, Function<Object, T> converter) {
        if (value == null) {
            return Collections.emptyList();
        }
        if (value instanceof String) {
            value = ((String) value).split(",");
        }

        if (value instanceof Object[]) {
            value = Arrays.asList(((Object[]) value));
        }
        if (value instanceof Collection) {
            return ((Collection<?>) value)
                .stream()
                .map(converter)
                .collect(Collectors.toList());
        }
        return Collections.singletonList(converter.apply(value));
    }

    /**
     * 转换参数为List
     *
     * @param value 参数
     * @return 排序后的流
     */
    public static List<Object> convertToList(Object value) {
        return convertToList(value, Function.identity());
    }

    /**
     * 根据排序参数对指定对Flux流进行排序
     *
     * @param flux  Flux
     * @param sorts 排序参数
     * @param <T>   流中元素类型
     * @return 排序后的流
     */
    public static <T> Flux<T> convertSortedStream(Flux<T> flux, Sort... sorts) {
        return convertSortedStream(flux, Arrays.asList(sorts));
    }

    /**
     * 根据排序参数对指定对Flux流进行排序
     *
     * @param flux  Flux
     * @param sorts 排序参数
     * @param <T>   流中元素类型
     * @return 排序后的流
     */
    public static <T> Flux<T> convertSortedStream(Flux<T> flux, Collection<Sort> sorts) {
        return convertSortedStream(flux, FastBeanCopier::getProperty, sorts);
    }

    /**
     * 根据排序参数对指定对Flux流进行排序
     *
     * @param flux           Flux
     * @param sorts          排序参数
     * @param propertyGetter 对比字段获取器,用于获取元素中的字段数据.
     * @param <R>            流中元素类型
     * @return 排序后的流
     */
    public static <R> Flux<R> convertSortedStream(Flux<R> flux,
                                                  BiFunction<R, String, Object> propertyGetter,
                                                  Collection<Sort> sorts) {
        if (CollectionUtils.isEmpty(sorts)) {
            return flux;
        }
        return flux.sort(convertComparator(sorts, propertyGetter));
    }

    /**
     * 将排序参数转为Comparator
     *
     * @param sorts 排序参数
     * @param <R>   元素类型
     * @return Comparator
     */
    public static <R> Comparator<R> convertComparator(Collection<Sort> sorts,
                                                      BiFunction<R, String, Object> propertyGetter) {
        if (CollectionUtils.isEmpty(sorts)) {
            return Comparator.comparing(k -> 0);
        }
        List<Comparator<R>> comparators = new ArrayList<>(sorts.size());
        for (Sort sort : sorts) {
            String column = sort.getName();
            Comparator<R> comparator = (left, right) -> {
                Object leftVal = propertyGetter.apply(left, column);
                Object rightVal = propertyGetter.apply(right, column);
                return CompareUtils.compare(leftVal, rightVal);
            };
            if (sort.getOrder().equalsIgnoreCase("desc")) {
                comparator = comparator.reversed();
            }
            comparators.add(comparator);

        }
        return ComparatorUtils.chainedComparator(comparators);
    }

    private static final String[] EMPTY_TAG = new String[0];

    private static final Map<Map<String, Object>, Tags> tagCache = new ConcurrentReferenceHashMap<>();

    public static Tags convertMapToTagsInfo(Map<String, Object> map) {
        if (MapUtils.isEmpty(map)) {
            return Tags.empty();
        }
        return tagCache.computeIfAbsent(map, m -> Tags.of(convertMapToTags(m)));
    }

    /**
     * 将Map转为tag,如果map中到值不是数字,则转为json.
     * <pre>
     *      {"key1":"value1","key2":["value2"]} => key,value1,key2,["value2"]
     *  </pre>
     *
     * @param map map
     * @return tags
     */
    @SneakyThrows
    public static String[] convertMapToTags(Map<String, Object> map) {
       return org.jetlinks.sdk.server.utils.ConverterUtils.convertMapToTags(map);
    }

    /**
     * 将对象转为查询条件,支持json和表达式格式,如:
     * <pre>
     *   //name = xxx and age > 10
     *   convertTerms("name is xxx and age gt 10")
     *
     *   convertTerms({"name":"xxx","age$gt":10})
     * </pre>
     *
     * @param value
     * @return 条件集合
     */
    @SuppressWarnings("all")
    @SneakyThrows
    public static List<Term> convertTerms(Object value) {
        return org.jetlinks.sdk.server.utils.ConverterUtils.convertTerms(value);
    }

    public static String byteBufToString(ByteBuf buf,
                                         Charset charset) {
        return StringBuilderUtils
            .buildString(
                buf, charset,
                (_buf, _charset, builder) -> byteBufToString(builder, _buf, _charset));
    }


    /**
     * 将字符串转为ByteBuf,字符串中包含\x开头的内容将按16进制解析为byte.
     *
     * @param str     字符串
     * @param charset 字符集
     */
    public static ByteBuf stringToByteBuf(CharSequence str, Charset charset) {
        ByteBuf buf = Unpooled.buffer(str.length());
        stringToByteBuf(str, buf, charset);
        return buf;
    }

    /**
     * 将字符串转为ByteBuf,字符串中包含\x开头的内容将按16进制解析为byte.
     *
     * @param str     字符串
     * @param buf     ByteBuf
     * @param charset 字符集
     */
    public static void stringToByteBuf(CharSequence str,
                                       ByteBuf buf,
                                       Charset charset) {

        int idx = 0;
        int len = str.length();
        while (idx < len) {
            char c = str.charAt(idx);
            if (c == '\\' && str.charAt(idx + 1) == 'x') {
                buf.writeByte(ByteBufUtil.decodeHexByte(str, idx + 2));
                idx += 4;
            } else {
                buf.writeCharSequence(String.valueOf(c), charset);
                idx++;
            }
        }

    }

    private static final Set<Character>
        visibleChar = new HashSet<>(
        Arrays.asList(
            ' ', '!', '"', '#', '$', '%', '&', '\'', '(', ')', '*', '+', ',', '-', '.', '/',
            '0', '1', '2', '3', '4', '5', '6', '7', '8', '9', ':', ';', '<', '=', '>', '?',
            '@', 'A', 'B', 'C', 'D', 'E', 'F', 'G', 'H', 'I', 'J', 'K', 'L', 'M', 'N', 'O', 'P', 'Q', 'R', 'S', 'T', 'U', 'V', 'W', 'X', 'Y', 'Z',
            '[', '\\', ']', '^', '_', '`',
            'a', 'b', 'c', 'd', 'e', 'f', 'g', 'h', 'i', 'j', 'k', 'l', 'm', 'n', 'o', 'p', 'q', 'r', 's', 't', 'u', 'v', 'w', 'x', 'y', 'z',
            '{', '|', '}', '~', '`', '[', ']', '{', '}', ';', '\'', ',', '.', '/', '?', '<', '>', '~', '!', '@', '#', '$', '%', '^', '&', '*', '(', ')', '-', '_', '+', '=', '|',
            '·', '！', '@', '#', '￥', '%', '…', '&', '*', '（', '）', '—', '+', '|', '：', '“', '”', '《', '》', '？', '、',
            '。', '，', '；', '‘', '’', '【', '】', '、', '·', '～', '·', '！', '@', '#', '￥', '%', '…', '&', '*', '（', '）',
            '—', '+', '|', '：', '“', '”'
        )
    );


    /**
     * 将netty的ByteBuf转为字符串,buf中包含非指定字符集的字符,则转换为16进制如: \x00
     *
     * @param builder StringBuilder 用于接收字符串
     * @param buf     ByteBuf
     * @param charset 字符集
     */
    public static void byteBufToString(StringBuilder builder,
                                       ByteBuf buf,
                                       Charset charset) {


        int len = buf.readableBytes();
        int idx = buf.readerIndex();

        CharsetEncoder encoder = CharsetUtil.encoder(charset);
        int avgPerChar = (int) encoder.averageBytesPerChar();

        int maxPerChar = (int) encoder.maxBytesPerChar();

        while (len > 0) {

            if (len >= avgPerChar && ByteBufUtil.isText(buf, idx, avgPerChar, charset)) {
                CharSequence cs = buf.getCharSequence(idx, avgPerChar, charset);
                int clen = cs.length();
                for (int i = 0; i < clen; i++) {
                    char c = cs.charAt(i);
                    if (visibleChar.contains(c)) {
                        builder.append(c);
                    } else {
                        builder
                            .append("\\x")
                            .append(ByteBufUtil.hexDump(buf, idx + i, 1));
                    }
                }
                len -= avgPerChar;
                idx += avgPerChar;
                continue;
            }


            if (len >= maxPerChar && ByteBufUtil.isText(buf, idx, maxPerChar, charset)) {
                CharSequence cs = buf.getCharSequence(idx, maxPerChar, charset);
                builder.append(cs);
                len -= maxPerChar;
                idx += maxPerChar;
                continue;
            }

            //不可识别的转为hex
            builder
                .append("\\x")
                .append(ByteBufUtil.hexDump(buf, idx, 1));
            len--;
            idx++;
        }
    }

    public static ByteBuf convertBuffer(Object obj) {
        return org.jetlinks.sdk.server.utils.ConverterUtils.convertNettyBuffer(obj);
    }

    public static boolean isHexEncoded(String str) {
            return StringUtils.hasText(str) &&
                str.length() % 2 == 0 &&
                HEX_PATTERN.matcher(str).matches();
        }
}

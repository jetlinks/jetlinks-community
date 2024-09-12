package org.jetlinks.community.io.utils;

import org.apache.commons.lang3.StringUtils;

import java.util.BitSet;

/**
 * @author bestfeng
 */
public class UrlCodecUtils {


    static BitSet dontNeedEncoding;

    static {
        dontNeedEncoding = new BitSet(128);
        int i;
        for (i = 'a'; i <= 'z'; i++) {
            dontNeedEncoding.set(i);
        }
        for (i = 'A'; i <= 'Z'; i++) {
            dontNeedEncoding.set(i);
        }
        for (i = '0'; i <= '9'; i++) {
            dontNeedEncoding.set(i);
        }
        dontNeedEncoding.set('+');
        dontNeedEncoding.set('-');
        dontNeedEncoding.set('_');
        dontNeedEncoding.set('.');
        dontNeedEncoding.set('*');
        dontNeedEncoding.set('%');
    }

    /**
     * 字符串是否经过了url encode
     *
     * @param text 字符串
     * @return true表示是
     */
    public static boolean hasEncode(String text) {
        if (StringUtils.isBlank(text)) {
            return false;
        }
        for (int i = 0; i < text.length(); i++) {
            int c = text.charAt(i);
            if (!dontNeedEncoding.get(c)) {
                return false;
            }
            if (c == '%' && (i + 2) < text.length()) {
                // 判断是否符合urlEncode规范
                char c1 = text.charAt(++i);
                char c2 = text.charAt(++i);
                if (!isDigit16Char(c1) || !isDigit16Char(c2)) {
                    return false;
                }
            }
        }
        return true;
    }

    /**
     * 判断c是否是16进制的字符
     *
     * @param c 字符
     * @return true表示是
     */
    private static boolean isDigit16Char(char c) {
        return (c >= '0' && c <= '9') || (c >= 'A' && c <= 'F');
    }
}

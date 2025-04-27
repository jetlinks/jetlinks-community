package org.jetlinks.community.utils;

/**
 * @author gyl
 * @since 2.0
 */
public class FormatUtils {

    private static final String[] formats = {"B", "KB", "MB", "GB"};

    /**
     * 单位为字节的大小转换为最大单位
     *
     * @param bytes
     * @return
     */
    public static String formatDataSize(long bytes) {
        int i = 0;
        float total = bytes;
        while (total >= 1024 && i < formats.length - 1) {
            total /= 1024;
            i++;
        }

        return String.format("%.2f%s", total, formats[i]);
    }


    /**
     * 将毫秒格式化为x小时x分钟x秒表示
     *
     * @param diffTime
     * @return
     */
    public static String calculateLifeTime(long diffTime) {
        long hours = diffTime / 3600000;
        long minutes = (diffTime % 3600000) / 60000;
        long seconds = (diffTime % 60000) / 1000;
        return hours + "小时" + minutes + "分钟" + seconds + "秒";
    }



}

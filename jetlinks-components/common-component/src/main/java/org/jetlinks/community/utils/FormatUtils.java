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

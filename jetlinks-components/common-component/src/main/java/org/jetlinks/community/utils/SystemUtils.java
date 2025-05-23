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

import java.util.function.Supplier;

public class SystemUtils {

    static float memoryWatermark = Float.parseFloat(
        System.getProperty("memory.watermark", System.getProperty("memory.waterline", "0.1")));

    //水位线持续
    static long memoryWatermarkDuration = TimeUtils
        .parse(System.getProperty("memory.watermark.duration", "5s"))
        .toMillis();

    static long errorPintInterval = TimeUtils
        .parse(System.getProperty("memory.watermark.duration", "500"))
        .toMillis();

    static Supplier<Float> memoryRemainderSupplier = () -> {
        Runtime rt = Runtime.getRuntime();
        long free = rt.freeMemory();
        long total = rt.totalMemory();
        long max = rt.maxMemory();
        return (max - total + free) / (max + 0.0F);
    };

    /**
     * 获取内存剩余比例,值为0-1之间,值越小,剩余可用内存越小
     *
     * @return 内存剩余比例
     */
    public static float getMemoryRemainder() {
        return memoryRemainderSupplier.get();
    }

    private static volatile long outTimes = 0;
    private static volatile long lastPrintTime = 0;

    /**
     * 判断当前内存是否已经超过水位线
     *
     * @return 是否已经超过水位线
     */
    public static boolean memoryIsOutOfWatermark() {
        boolean out = getMemoryRemainder() < memoryWatermark;
        if (!out) {
            outTimes = 0;
            return false;
        }
        //连续超水位线
        if (outTimes == 0) {
            outTimes = System.currentTimeMillis();
        } else {
            if(System.currentTimeMillis() - outTimes > memoryWatermarkDuration){
                System.gc();
                return true;
            }
        }
        return false;
    }

    /**
     * 直接打印消息到控制台,支持格式化,如<code>printError("save error %s",id);</code>
     *
     * @param format 格式化
     * @param args   格式化参数
     * @see java.util.Formatter
     */
    public static void printError(String format, Object... args) {
        printError(format, () -> args);
    }

    /**
     * 直接打印消息到控制台,支持格式化,如<code>printError("save error %s",id);</code>
     *
     * @param format      格式化
     * @param argSupplier 格式化参数
     * @see java.util.Formatter
     */
    public static void printError(String format, Supplier<Object[]> argSupplier) {
        long now = System.currentTimeMillis();
        //防止频繁打印导致线程阻塞
        if (now - lastPrintTime > errorPintInterval) {
            lastPrintTime = now;
            System.err.printf((format) + "%n", argSupplier.get());
        }
    }
}

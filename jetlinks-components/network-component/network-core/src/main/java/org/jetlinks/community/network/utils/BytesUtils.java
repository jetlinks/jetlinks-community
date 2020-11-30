package org.jetlinks.community.network.utils;

/**
 * 字节转换工具
 *
 * @author zhouhao
 * @see org.jetlinks.core.utils.BytesUtils
 */
@Deprecated
public class BytesUtils {

    /**
     * 高位字节数组转int,低字节在前.
     * -------------------------------------------
     * |  0-7位  |  8-16位  |  17-23位  |  24-31位 |
     * -------------------------------------------
     *
     * @param src 字节数组
     * @return int值
     */
    public static int highBytesToInt(byte[] src) {
        return highBytesToInt(src, 0, src.length);
    }

    /**
     * 高位字节数组转long,低字节在前.
     * -------------------------------------------
     * |  0-7位  |  8-16位  |  17-23位  |  24-31位 |
     * -------------------------------------------
     *
     * @param src 字节数组
     * @return int值
     */
    public static int highBytesToInt(byte[] src, int offset, int len) {
        int n = 0;
        len = Math.min(len, 4);
        for (int i = 0; i < len; i++) {
            int left = i * 8;
            n += ((src[i + offset] & 0xFF) << left);
        }
        return n;
    }


    public static long highBytesToLong(byte[] src, int offset, int len) {
        long n = 0;
        len = Math.min(Math.min(len, src.length), 8);
        for (int i = 0; i < len; i++) {
            int left = i * 8;
            n += ((long)(src[i + offset] & 0xFF) << left);
        }
        return  n;
    }

    /**
     * 低位字节数组转int,低字节在后.
     * -------------------------------------------
     * |  31-24位 |  23-17位   |  16-8位 |   7-0位 |
     * -------------------------------------------
     *
     * @param src 字节数组
     * @return int值
     */
    public static int lowBytesToInt(byte[] src, int offset, int len) {
        int n = 0;
        len = Math.min(len, 4);
        for (int i = 0; i < len; i++) {
            int left = i * 8;
            n += ((src[offset + len - i - 1] & 0xFF) << left);
        }
        return n;
    }

    /**
     * 低位字节数组转long,低字节在后.
     * -------------------------------------------
     * |  31-24位 |  23-17位   |  16-8位 |   7-0位 |
     * -------------------------------------------
     *
     * @param src 字节数组
     * @return int值
     */
    public static long lowBytesToLong(byte[] src, int offset, int len) {
        long n = 0;
        len = Math.min(len, 4);
        for (int i = 0; i < len; i++) {
            int left = i * 8;
            n += ((long)(src[offset + len - i - 1] & 0xFF) << left);
        }
        return n;
    }

    /**
     * 低位字节数组转int,低字节在后
     * -------------------------------------------
     * |  31-24位 |  23-17位   |  16-8位 |   7-0位 |
     * -------------------------------------------
     *
     * @param src 字节数组
     * @return int值
     */
    public static int lowBytesToInt(byte[] src) {

        return lowBytesToInt(src, 0, src.length);
    }

    /**
     * int转高位字节数组,低字节在前
     * -------------------------------------------
     * |  0-7位  |  8-16位  |  17-23位  |  24-31位 |
     * -------------------------------------------
     *
     * @param src 字节数组
     * @return bytes 值
     */
    public static byte[] toHighBytes(byte[] target, long src, int offset, int len) {
        for (int i = 0; i < len; i++) {
            target[offset + i] = (byte) (src >> (i * 8) & 0xff);
        }
        return target;
    }


    /**
     * int转高位字节数组,低字节在前
     * -------------------------------------------
     * |  0-7位  |  8-16位  |  17-23位  |  24-31位 |
     * -------------------------------------------
     *
     * @param src 字节数组
     * @return bytes 值
     */
    public static byte[] toHighBytes(int src) {
        return toHighBytes(new byte[4], src, 0, 4);
    }


    /**
     * 转低位字节数组, 低字节在后
     * --------------------------------------------
     * |  31-24位 |  23-17位   |  16-8位 |   7-0位 |
     * --------------------------------------------
     *
     * @param src 字节数组
     * @return int值
     */
    public static byte[] toLowBytes(byte[] target, long src, int offset, int len) {
        for (int i = 0; i < len; i++) {
            target[offset + len - i - 1] = (byte) (src >> (i * 8) & 0xff);
        }
        return target;
    }

    /**
     * int转低位字节数组, 低字节在后
     * --------------------------------------------
     * |  31-24位 |  23-17位   |  16-8位 |   7-0位 |
     * --------------------------------------------
     *
     * @param src 字节数组
     * @return int值
     */
    public static byte[] toLowBytes(int src) {
        return toLowBytes(new byte[4], src, 0, 4);
    }

    public static byte[] toLowBytes(long src) {
        return toLowBytes(new byte[8], src, 0, 8);
    }

}

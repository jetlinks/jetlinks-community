//@ts-ignore
import {java} from "java";

/**
 * 操作java的相关接口
 */
declare class Java {
    /**
     * 获取java class,如: Java.type('int[]');
     * @param clazz 类型名称
     */
    static type(clazz: string): object;
}

declare var utils: Utils;

/**
 * 内置工具类
 */
declare interface Utils {

    /**
     * 获取当前UTC时间戳
     */
    // @ts-ignore
    now(): long;

    /**
     * 创建java byte 数组
     *
     * let arr =  utils.newByteArray(8);
     * arr[0]= 0x01;
     *
     * @param size 长度
     */
    // @ts-ignore
    newByteArray(size: number): byte[];

    /**
     * 创建java int 数组
     *
     * let arr = utils.newIntArray(8);
     * arr[0]= 0x01;
     *
     * @param size 长度
     */
    // @ts-ignore
    newIntArray(size: number): int[];

    /**
     * 将js对象转为java对象
     *
     * utils.toJavaType({"key":"value"});//转为java.util.Map
     *
     * utils.toJavaType([1,2,3]);//转为java.util.List
     *
     * @param jsObject js对象
     */
    toJavaType(jsObject: object): object;

    /**
     * 对字符串进行md5摘要,返回md5 16进制字符串
     *
     * let md5 =  utils.md5Hex(id+"|"+timestamp);
     *
     * @param str 要进行md5摘要的字符串
     */
    md5Hex(str: string): string;

    /**
     * 对字符串进行sha1摘要,返回sha1 16进制字符串
     *
     * let sha1 =  utils.sha1Hex(id+"|"+timestamp);
     *
     * @param str 要进行sha1摘要的字符串
     */
    sha1Hex(str: string): string;

    /**
     * 高精度加法,返回: java.math.BigDecimal
     *
     * let val =  sum(value,10.23).doubleValue();
     *
     * @param numbers
     */
    sum(...numbers: number[]): java.math.BigDecimal;

    /**
     * 高精度减法,返回: java.math.BigDecimal.
     *
     *  let val =  sub(value,10.23).doubleValue();
     *
     * @param numbers
     */
    sub(...numbers: number[]): java.math.BigDecimal;

    /**
     * 高精度乘法,返回: java.math.BigDecimal.
     *
     *  let val =  mul(value,10.23).doubleValue();
     *
     * @param numbers
     */
    mul(...numbers: number[]): java.math.BigDecimal;


    /**
     * 高精度除法,返回: java.math.BigDecimal.
     *
     *  let val =  div(value,10.23).doubleValue();
     *
     * @param numbers
     */
    div(...numbers: number[]): java.math.BigDecimal;

    /**
     * 对比大小,返回最大值
     *
     *  let val = max(a,b,c);
     *
     * @param objects 要对比的值
     */
    max(...objects: object[]): object;

    /**
     * 对比大小,返回最小值
     *
     *  let val = min(a,b,c);
     *
     * @param objects 要对比的值
     */
    min(...objects: object[]): object;

    /**
     * 解析json字符串为对象
     * @param jsonString JSON字符串
     */
    parseJson(jsonString: string): object;

    /**
     * 将对象转换为json字符串
     * @param obj 对象
     */
    toJsonString(obj: object): string;
}
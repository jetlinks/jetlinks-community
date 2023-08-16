//@ts-ignore
import {java} from "java";



declare module io.vertx.core.buffer.Buffer{

    class Unpooled {
        static buffer(): Buffer;
    }

    class Buffer {

        /**
         * 返回Buffer中pos位置的字节
         * @param pos
         */
        //@ts-ignore
        getByte(pos: int): byte;

        /**
         * 返回Buffer中pos位置的无符号字节，作为short
         * @param pos
         */
        //@ts-ignore
        getUnsignedByte(pos: int): short;


        /**
         * 返回Buffer中pos位置的int值
         * @param pos
         */
        //@ts-ignore
        getInt(pos: int): int;

        /**
         * 以小端字节序获取该缓冲区中指定绝对索引处的32位整数
         * @param pos
         */
        //@ts-ignore
        getIntLE(pos: int): int;

        /**
         * 返回Buffer中pos位置的无符号整型，作为long
         * @param pos
         */
        //@ts-ignore
        getUnsignedInt(pos: int): long;

        /**
         * 返回Buffer中pos位置的无符号整型，作为小端字节顺序中的long
         * @param pos
         */
        //@ts-ignore
        getUnsignedIntLE(pos: int): long;


        /**
         * 返回缓冲区中位于pos位置的长值
         * @param pos
         */
        //@ts-ignore
        getLong(pos: int): long;

        /**
         * 以小端字节序获取该缓冲区中指定的绝对索引处的64位长整数
         * @param pos
         */
        //@ts-ignore
        getLongLE(pos: int): long;

        /**
         * 返回Buffer中pos位置的double值
         * @param pos
         */
        //@ts-ignore
        getDouble(pos: int): double;

        /**
         * 返回Buffer中pos位置的浮点数
         * @param pos
         */
        //@ts-ignore
        getFloat(pos: int): float;

        /**
         * 返回缓冲区中pos位置的short
         * @param pos
         */
        //@ts-ignore
        getShort(pos: int): short;

        /**
         * 以小端字节序在此缓冲区中指定的绝对索引处获取一个16位短整数
         * @param pos
         */
        //@ts-ignore
        getShortLE(pos: int): short;

        /**
         * 返回Buffer中pos位置的无符号short，作为整型
         * @param pos
         */
        //@ts-ignore
        getUnsignedShort(pos: int): int;

        /**
         * 以小端字节序获取该缓冲区中指定绝对索引处的无符号16位短整数
         * @param pos
         */
        //@ts-ignore
        getUnsignedShortLE(pos: int): int;

        /**
         * 返在此缓冲区中指定的绝对索引处获取一个24位中等整数
         * @param pos
         */
        //@ts-ignore
        getMedium(pos: int): int;

        /**
         * 以小端字节序获取该缓冲区中指定绝对索引处的24位中等整数
         * @param pos
         */
        //@ts-ignore
        getMediumLE(pos: int): int;

        /**
         * 在此缓冲区中指定的绝对索引处获取无符号24位中整数
         * @param pos
         */
        //@ts-ignore
        getUnsignedMedium(pos: int): int;

        /**
         * 以小端字节序获取该缓冲区中指定绝对索引处的无符号24位中整数
         * @param pos
         */
        //@ts-ignore
        getUnsignedMediumLE(pos: int): int;

        /**
         * 以字节[]的形式返回整个Buffer的副本
         */
        //@ts-ignore
        getBytes(): byte[];

        /**
         * 返回Buffer子序列的副本，作为字节[]，从位置start开始，结束于位置end - 1
         */
        //@ts-ignore
        getBytes(start: int,end: int): byte[];

        /**
         * 返回Buffer子序列的副本，作为字节[]，从位置start开始，结束于位置end - 1
         */
        //@ts-ignore
        getBytes(start: int,end: int): byte[];


    }
}

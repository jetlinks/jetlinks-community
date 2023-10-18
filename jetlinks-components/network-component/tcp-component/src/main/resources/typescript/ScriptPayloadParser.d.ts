//@ts-ignore
import {java} from "java";
//@ts-ignore
import {} from "vertx";

//@ts-ignore
declare var parser: PipePayloadParser;


declare interface PipePayloadParser {

    /**
     * 读取固定长度的报文
     * @param size
     */
    //@ts-ignore
    fixed(size: int): PipePayloadParser;


    /**
     * 处理已读取的报文
     * ```js
     *  parser.handler((buf,pipParser)=>{
     *    let len = buf.getInt(0);
     *    pipParser.result(buf)
     *             .fixed(len);
     * });
     * ```
     * @param call
     */
    //@ts-ignore
    handler(call: (buf: io.vertx.core.buffer.Buffer, pipParser: PipePayloadParser) => void): PipePayloadParser;


    /**
     * 设置数据到结果中
     * @param buf
     */
    //@ts-ignore
    result(buf: string | byte[] | io.vertx.core.buffer.Buffer): PipePayloadParser;

    /**
     * 根据分割符分割报文
     * @param delimited
     */
    // @ts-ignore
    delimited(delimited: String): PipePayloadParser;


    /**
     * 完成本次读取，输出结果，开始下一次读取
     */
    //@ts-ignore
    complete(): PipePayloadParser;

}

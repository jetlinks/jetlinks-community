// @ts-ignore
import {org} from 'jetlinks';
// @ts-ignore
import {io} from 'netty';
// @ts-ignore
import {java} from 'java';
// @ts-ignore
import {} from 'engine-utils'

/**
 * 解码上下文,可获取设备上行数据相关信息
 */
declare interface DecodeContext {
    /**
     * 获取设备上报的透传数据
     */
    payload(): io.netty.buffer.ByteBuf;

    /**
     * 获取设备上报消息的时间戳
     */
    timestamp(): number;

    /**
     * 解析报文为json对象
     */
    json(): java.util.Map<string, any>;

    /**
     * 解析报文为json对象
     */
    jsonArray(): java.util.List<any>;

    /**
     * 获取原始透传消息
     */
    message(): org.jetlinks.core.message.DirectMessage;

    /**
     * 获取header中的url,通常在http透传时使用,需要在协议包中进行设置
     */
    url(): string;

    /**
     * 获取header中的topic,通常在mqtt透传时使用,需要在协议包中进行设置
     */
    topic(): string;

    /**
     * 解析路径中的变量信息例如:
     *  ```js
     *  var deviceId =  pathVars('/{deviceId}/#',context.topic()).get("deviceId");
     *  ```
     * @param pattern 模版
     * @param path 路径
     */
    pathVars(pattern: string, path: string): java.util.Map<string, string>;
}

/**
 * 编码上下文,进行设备下行消息相关操作
 */
declare interface EncodeContext {
    /**
     * 获取平台下发的原始消息
     */
    message(): org.jetlinks.core.message.W

    /**
     * 设置topic,通常在MQTT透传时使用,协议包内可通过message.getHeader("topic")获取
     * @param topic TOPIC
     */
    topic(topic: string): EncodeContext;

    /**
     * 获取即将发送给设备的透传报文
     */
    payload(): io.netty.buffer.ByteBuf;

    /**
     * 创建新的ByteBuf,用于构造报文
     */
    newBuffer(): io.netty.buffer.ByteBuf;

    /**
     * 设置发送给设备的透传报文,支持传入ByteBuf和string,如果是16进制需要以0x开头
     * @param hexString
     */
    setPayload(hexString: string | io.netty.buffer.ByteBuf): EncodeContext;

    /**
     * 声明当调用功能时的报文构造逻辑,如:
     *
     *  whenFunction('*',function(args){
     *
     *      return "0x500120FF";
     *  })
     *
     * @param functionId 物模型功能ID,如果写*则表示任意功能都会使用此逻辑
     * @param call 构造逻辑函数
     */
    whenFunction(functionId: string, call: (args: java.util.Map<string, any>) => string | io.netty.buffer.ByteBuf): EncodeContext;

    /**
     * 声明当修改属性时的报文构造逻辑,如:
     * <pre>
     *  whenWriteProperty('*',function(args){
     *
     *      return "0x500120FF";
     *  })
     *</pre>
     * @param propertyId 物模型属性ID,如果写*则表示属性都会使用此逻辑
     * @param call 构造逻辑函数
     */
    whenWriteProperty(propertyId: string, call: (value: any) => string | io.netty.buffer.ByteBuf): EncodeContext;

    /**
     * 声明当读取多个属性时的报文构造逻辑,如:
     *
     *  whenReadProperties('*',function(properties){
     *
     *      return "0x500120FF";
     *  })
     *
     * @param propertyId 物模型属性ID,如果写*则表示属性都会使用此逻辑
     * @param call 构造逻辑函数
     */
    whenReadProperties(propertyId: string, call: (value: java.util.List<string>) => string | io.netty.buffer.ByteBuf): EncodeContext;

    /**
     * 声明当读取单个属性时的报文构造逻辑,如:
     *
     *  whenReadProperties('temp',function(){
     *
     *      return "0x500120FF";
     *  })
     *
     * @param propertyId 物模型属性ID,如果写*则表示属性都会使用此逻辑
     * @param call 构造逻辑函数
     */
    whenReadProperty(propertyId: string, call: (value: string) => string | io.netty.buffer.ByteBuf): EncodeContext;


}

/**
 * 设备消息对象，如:
 * {
 *     "messageType":"REPORT_PROPERTY",
 *     "properties":{"temp":32.8}
 * }
 */
    //@ts-ignore
declare class MessageMap {

    /**
     * 消息类型
     *
     * REPORT_PROPERTY: 属性上报
     *
     * EVENT: 事件上报
     *
     * READ_PROPERTY_REPLY: 读取属性回复
     *
     * WRITE_PROPERTY_REPLY: 修改属性回复
     *
     * FUNCTION_INVOKE_REPLY: 功能调用回复
     *
     */
    messageType:
        /**
         * 属性上报
         */
        "REPORT_PROPERTY" |
        /**
         *  事件上报
         */
        "EVENT" |
        /**
         * 读取属性回复
         */
        "READ_PROPERTY_REPLY" |
        /**
         * 修改属性回复
         */
        "WRITE_PROPERTY_REPLY" |
        /**
         * 功能调用回复
         */
        "FUNCTION_INVOKE_REPLY";

    /**
     * 消息ID,如果是回复指令,需要设置为和平台下发时的一致
     */
    messageId: string;

    /**
     * 属性列表,当返回属性上报消息时设置
     */
        //@ts-ignore
    properties: PropertyMetadata | object;

    /**
     * 功能调用回复,当返回功能调用时设置
     */
        //@ts-ignore
    output:  object;

    /**
     * 事件ID,当返回事件上报消息时设置
     */
    event: string;

    /**
     * 事件数据,当返回事件上报消息时设置.
     */
        //@ts-ignore
    data: EventType | object
}

declare interface CodecContext {

    /**
     * 注册设备上行数据监听器,当设备上行数据时,回调将被调用,用于解析设备上报的数据.
     * @param call 回调函数
     */
    onUpstream(call: (context: DecodeContext) => MessageMap): CodecContext;

    /**
     * 注册设备下行数据监听器,当平台下发指令给设备时,回调将被调用,用于构造下发给设备的报文
     * @param call 回调函数
     */
    onDownstream(call: (context: EncodeContext) => void): CodecContext;


}

/**
 * 透传消息编解码内置变量,用于注册上下行监听器等操作
 */
declare var codec: CodecContext
/**
 * 子设备消息模拟,请在启动脚本start.sh中自行修改scriptFile选项
 *
 * 在平台中创建对应的设备实例:
 *
 * 父设备ID使用: gateway-1
 * 子设备ID使用: child-device-1
 * 型号使用演示型号.
 * 协议使用最新的demo-protocol-1.0.jar,源代码地址: https://github.com/jetlinks/demo-protocol
 */
var _logger = logger;

//事件类型
var events = {
    reportProperty: function (index, session) {
        var deviceId = "child-device-1";
        var topic = "/children/report-property";
        var json = JSON.stringify({
            "deviceId": deviceId,
            "success": true,
            "timestamp": new Date().getTime(),
            properties: {"temperature": java.util.concurrent.ThreadLocalRandom.current().nextDouble(20, 40)},
        });
        session.sendMessage(topic, json)
    },
    fireAlarm: function (index, session) {
        var deviceId = "child-device-1";
        var topic = "/children/fire_alarm";
        var json = JSON.stringify({
            "deviceId": deviceId, // 设备编号 "pid": "TBS-110", // 设备编号
            "a_name": "商务大厦", // 区域名称 "bid": 2, // 建筑 ID
            "b_name": "C2 栋", // 建筑名称
            "l_name": "12-05-201", // 位置名称
            "timestamp": new Date().getTime() // 消息时间
        });

        session.sendMessage(topic, json)
    }
};

//事件上报
simulator.onEvent(function (index, session) {
    //上报属性
    events.reportProperty(index, session);

    //上报火警
    events.fireAlarm(index, session);
});

simulator.bindHandler("/children/read-property", function (message, session) {
    _logger.info("读取子设备属性:[{}]", message);
    session.sendMessage("/read-property-reply", JSON.stringify({
        messageId: message.messageId,
        deviceId: message.deviceId,
        timestamp: new Date().getTime(),
        properties: {"temperature": java.util.concurrent.ThreadLocalRandom.current().nextDouble(20, 40)},
        success: true
    }));
});


simulator.onConnect(function (session) {
    //模拟子设备上线
    session.sendMessage("/children/device_online_status", JSON.stringify({
        deviceId: "child-device-1",
        timestamp: new Date().getTime(),
        status: "1",
        success: true
    }));
});

simulator.onAuth(function (index, auth) {
    //使用网关设备id 连接平台
    auth.setClientId("gateway-1" );
    auth.setUsername("admin");
    auth.setPassword("admin");
});
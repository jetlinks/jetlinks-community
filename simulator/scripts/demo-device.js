/**
 * 烟感设备模拟器
 */
var _logger = logger;
//设备实例id前缀
var devicePrefix = "demo-";

var eventId = Math.ceil(Math.random() * 1000);
//事件类型
var events = {
    reportProperty: function (index, session) {
        var deviceId = session.auth.clientId;
        var topic = "/report-property";
        var json = JSON.stringify({
            "deviceId": deviceId,
            "success": true,
            "timestamp": new Date().getTime(),
            properties: {"temperature": java.util.concurrent.ThreadLocalRandom.current().nextInt(20, 30)},
        });
        session.sendMessage(topic, json)
    },
    fireAlarm: function (index, session) {
        var deviceId = session.auth.clientId;
        var topic = "/fire_alarm/department/1/area/1/dev/" + deviceId;
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

//读取属性
simulator.bindHandler("/read-property", function (message, session) {
    _logger.info("读取属性:[{}]", message);
    session.sendMessage("/read-property-reply", JSON.stringify({
        messageId: message.messageId,
        deviceId: message.deviceId,
        timestamp: new Date().getTime(),
        properties: {"temperature": java.util.concurrent.ThreadLocalRandom.current().nextInt(20, 30)},
        success: true
    }));
});

//读取子设备属性
simulator.bindHandler("/children/read-property", function (message, session) {
    _logger.info("读取子设备属性:[{}]", message);
    session.sendMessage("/children/read-property-reply", JSON.stringify({
        messageId: message.messageId,
        deviceId: message.deviceId,
        timestamp: new Date().getTime(),
        properties: {"temperature": java.util.concurrent.ThreadLocalRandom.current().nextInt(20, 30)},
        success: true
    }));
});

//调用功能
simulator.bindHandler("/invoke-function", function (message, session) {
    _logger.info("调用功能:[{}]", message);
    session.sendMessage("/invoke-function", JSON.stringify({
        messageId: message.messageId,
        deviceId: message.deviceId,
        timestamp: new Date().getTime(),
        output: "ok", //返回结果
        success: true
    }));
});

//修改属性
simulator.bindHandler("/write-property", function (message, session) {
    var reply = com.alibaba.fastjson.JSON.toJSONString({
        messageId: message.messageId,
        deviceId: message.deviceId,
        timestamp: new Date().getTime(),
        properties: new java.util.HashMap(message.properties),
        success: true
    });
    _logger.info("修改属性:{}\n{}", message,reply);

    session.sendMessage("/write-property",reply);
});


simulator.onConnect(function (session) {
    //自动绑定下级设备
    // session.sendMessage("/children/register", JSON.stringify({
    //     deviceId: "test202278", //子设备ID
    //     timestamp: new Date().getTime(),
    //     success: true
    // }));
    //注销子设备
    // simulator.runDelay(function () {
    //     session.sendMessage("/children/unregister", JSON.stringify({
    //         deviceId: "test202278",
    //         timestamp: new Date().getTime(),
    //         success: true
    //     }));
    // },2000)
});

simulator.onAuth(function (index, auth) {

    auth.setClientId(devicePrefix + index);
    auth.setUsername("admin");
    auth.setPassword("admin");
});
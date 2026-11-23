# 网关模块

统一管理设备网关服务,消息网关.

## TCP 会话注册

TCP 服务端网关在创建设备会话时，先挂载当前连接，再交给 `DeviceSessionManager` 注册，保证上线事件处理期间查询会话时不会因连接列表为空而误判离线。已有会话仍通过会话回调接入后续连接，同一连接重复挂载由 `registerConnection` 去重。

实现与回归测试位于 `jetlinks-components/network-component/tcp-component` 下的 `TcpServerDeviceGateway.java` 和 `TcpServerDeviceGatewayTest.java`。

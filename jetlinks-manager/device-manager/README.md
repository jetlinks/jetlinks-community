# Device Manager

## 设备消息链路追踪

设备接入链路在通用消息边界维护追踪上下文，避免由具体协议或诊断场景分别补偿：

- `DeviceMessageConnector.handleMessage` 在发布设备消息前将当前 trace context 写入消息头，使异步消息消费者能够继续同一条链路。
- `DeviceMessageConnector.doReply` 从回复消息头恢复父上下文，并创建 `/device/{deviceId}/response` span。
- `TransparentDeviceMessageConnector` 为绕过 `DeviceGatewayHelper` 的普通透传消息创建 `/device/{deviceId}/handle` span；子设备透传仍由 `DeviceGatewayHelper` 统一追踪，避免重复 span。

设备消息侧只增加链路 span 和上下文透传；协议 Monitor 上下文由 `jetlinks-components/protocol-component` 统一提供。本次不包含 gateway/network monitor、身份标识、集群 tracing 或诊断日志持久化能力。

验证命令：

```bash
mvn -pl jetlinks-components/protocol-component,jetlinks-manager/device-manager -am \
  -Dtest=ProtocolMonitorHelperTest,DeviceMessageConnectorTracingTest,TransparentDeviceMessageConnectorTracingTest \
  -Dsurefire.failIfNoSpecifiedTests=false test
```

验证结果：设备链路追踪 3 个用例通过，`device-manager` 及其上游 Reactor 模块构建成功。

交付记录：实现提交 `dbb96410`，异常恢复测试提交 `d80628b3`，Pull Request [#761](https://github.com/jetlinks/jetlinks-community/pull/761)。

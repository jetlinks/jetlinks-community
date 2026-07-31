# Protocol Component

## 协议 Monitor 上下文

目标是在协议执行链路中支持临时切换设备级 `Monitor`，使协议包通过 `ServiceContext` 获取到的代理 Monitor 能随 Reactor 上下文传播，并将协议 span 和日志归属到当前诊断设备。

影响范围：

- `ProtocolMonitorHelper`：代理 Monitor、当前 Monitor 管理以及同步、`Mono`、`Flux` 执行包装。
- `ProtocolMonitorThreadLocalAccessor`：在 Micrometer context propagation 与 Netty `FastThreadLocal` 之间传递当前 Monitor。
- `META-INF/services/io.micrometer.context.ThreadLocalAccessor`：注册上下文访问器。

不修改协议报文、编解码规则或传输路由，也不迁移 gateway/network monitor、指标采集和网络运行状态监控。

验证命令：

```bash
mvn -pl jetlinks-components/protocol-component,jetlinks-manager/device-manager -am \
  -Dtest=ProtocolMonitorHelperTest \
  -Dsurefire.failIfNoSpecifiedTests=false test
```

验证结果：`ProtocolMonitorHelperTest` 4 个用例通过，相关 Reactor 模块全部构建成功。

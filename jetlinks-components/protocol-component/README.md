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

验证结果：`ProtocolMonitorHelperTest` 5 个用例通过，相关 Reactor 模块全部构建成功。

交付记录：实现提交 `dbb96410`，异常恢复测试提交 `d80628b3`，Pull Request [#761](https://github.com/jetlinks/jetlinks-community/pull/761)。

## Trace 上下文与日志关联

### 目标与影响范围

协议编解码的 OpenTelemetry Context 已保存在 Reactor Context 中，但社区版尚未将其恢复为执行线程的当前 Context，导致协议包调用 `context.getMonitor().logger()` 时，`LogRecord` 通过 `Span.current()` 无法取得 `traceId`。本次从公共上下文传播入口修复，使协议诊断日志和系统日志都能关联当前有效 span。

Owning module 为 `jetlinks-components/common-component`，同时涉及：

- `common-component`：实现 OpenTelemetry `ContextStorageProvider` 与 Micrometer `ThreadLocalAccessor` 的统一适配，并开启 Reactor 自动上下文传播。
- `logging-component`：系统日志转换时从当前有效 span 写入 `traceId`、`spanId`。
- `protocol-component`：保留现有 Monitor 传播逻辑，仅增加回归验证，不在协议包内手工传递 traceId。

不新增业务 span，不修改协议报文、编解码、设备消息路由或日志 Topic；不迁移 gateway/network monitor、指标采集、诊断日志持久化、集群 tracing、身份标识或其他 monitor 能力。不新增 MBean、i18n 或新的三方依赖。

### 实现入口

1. `common-component` 使用 OpenTelemetry 与 Micrometer 官方 SPI 注册通用 Context 存储和线程上下文访问器，并由公共自动配置开启 Reactor 自动上下文传播。
2. 上下文在订阅、跨调度器信号、正常完成、异常和取消后恢复，嵌套 Scope 乱序关闭不会覆盖当前有效上下文。
3. `logging-component` 在系统日志转换时投影当前有效 span 的 `traceId`、`spanId`；无有效 span 时保持字段为空，不伪造链路标识。
4. `protocol-component` 通过真实 `Monitor.logger()` 回归用例验证协议诊断日志跨调度器仍可关联同一 trace。

### 风险与验证

- OpenTelemetry `ContextStorageProvider` 在 JVM 内按 SPI 初始化；测试已验证服务注册、嵌套 Scope 恢复和乱序关闭保护。
- Reactor 自动上下文传播为进程级能力，会恢复所有已注册的 `ThreadLocalAccessor`；测试已验证跨线程传播及正常、异常、取消后的清理，未改变业务信号和错误传播语义。
- 不新增重复埋点；现有 `MonoTracer`、`FluxTracer` 和设备领域 span 继续作为唯一 span 来源。

验证命令：

```bash
mvn -pl jetlinks-components/common-component,jetlinks-components/logging-component,jetlinks-components/protocol-component,jetlinks-manager/device-manager -am \
  -Dtest=TraceContextPropagationTest,SystemLoggingAppenderTest,ProtocolMonitorHelperTest,DeviceMessageConnectorTracingTest,TransparentDeviceMessageConnectorTracingTest \
  -Dsurefire.failIfNoSpecifiedTests=false test
```

验证结果：5 个目标测试类共 16 个用例通过，0 failed、0 skipped，19 个 Reactor 模块构建成功。跨调度器执行时 `Span.current()`、协议 `LogRecord` 和系统日志可取得当前 traceId/spanId；无有效 span 时字段为空；上下文在正常、异常、取消和嵌套 Scope 结束后均正确恢复。新增 `ThreadLocalContextStorageProvider` 的 JaCoCo line/branch 覆盖率均为 100%。

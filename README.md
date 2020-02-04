# JetLinks 物联网基础平台

JetLinks 是一个物联网基础平台,用于快速建立物联网相关业务系统.

- 集成了各种常见的网络协议(MQTT,HTTP,TCP,UDP,CoAP)等,并对其进行封装,
实现统一管理,监控,在线调试,在线启动,停止,更新等功能.降低网络编程的复杂度.

- 灵活的设备接入方式. 动态配置设备网关服务.

- 多消息协议支持,可在线配置消息解析规则,将自定义的消息解析为平台统一的消息格式.

- 统一的设备操作API,屏蔽各个厂家`不同协议`不同设备的差异,支持`跨服务`,同步(RRpc),异步的设备消息收发.

- 可视化拖拽规则引擎设计器,灵活可拓展的多种规则节点支持,可通过规则引擎在线动态配置数据,业务处理逻辑.

官方QQ群: `2021514`

# 技术栈

1. [Spring Boot 2.2.x](https://spring.io/projects/spring-boot)
2. [Spring WebFlux](https://spring.io/) 响应式Web支持
3. [R2DBC](https://r2dbc.io/) 响应式关系型数据库驱动
4. [Project Reactor](https://projectreactor.io/) 响应式编程框架
4. [Netty](https://netty.io/),[Vert.x](https://vertx.io/) 高性能网络编程框架
5. [ElasticSearch](https://www.elastic.co/cn/products/enterprise-search) 全文检索，日志，时序数据存储
6. [PostgreSQL](https://www.postgresql.org) 业务功能数据管理
7. [hsweb framework 4](https://github.com/hs-web) 业务功能基础框架

# 模块

```bash
--jetlinks-community
------|----docker
------|------|----dev-env       # 启动开发环境
------|------|----run-all       # 启动全部,通过http://localhost:9000 访问系统.
------|----jetlinks-components  # 公共组件模块
------|----jetlinks-manager     # 管理模块
------|----jetlinks-standalone  # 单点方式启动服务
------|----simulator            # 设备模拟器
```

# 文档

[快速开始](http://doc.jetlinks.cn/user-guide/quick-start.html)

# 许可版本

|  功能  |  社区版   | 专业版  |   企业版  |
| ----   |  ----  |   ----    |   -----   |
| 开放源代码      |  ✅ | ✅ |       ✅     |
| 设备管理,设备接入|  ✅ | ✅ |       ✅     |
| 多消息协议支持|  ✅ | ✅ |       ✅     |
| 规则引擎        |  ✅ |  ✅ |     ✅     |
| 邮件消息通知    |  ✅  |  ✅ |     ✅      |
| 微信企业消息    |  ✅  |  ✅ |     ✅      |
| 钉钉消息通知    |  ✅  |  ✅ |     ✅      |
| MQTT(TLS)    |  ✅  |  ✅ |   ✅   |
| TCP(TLS)     |  ✅  |  ✅ |  ✅    |
| CoAP(DTLS)    |  ⭕  |  ✅ |     ✅       |
| Http,WebSocket(TLS) |  ⭕  |  ✅ |     ✅ |
| 监控     |  ⭕  |  ✅ |  ✅   |
| 可视化图表配置   |  ⭕  |  ✅ |     ✅    |
| OpenAPI    |  ⭕  |  ✅ |     ✅     |
| 集群支持    |  ⭕  |  ✅ |     ✅     |
| 线上技术支持 |  ⭕  |  ✅ |   ✅   |
| 线下技术支持 |  ⭕  |  ⭕ |   ✅   |
| 定制开发   |  ⭕  |  ⭕ |   ✅   |
| 商业限制   |  无  |  单个项目 |   无   |
| 定价   |  免费  |  孵化中待定 |   孵化中待定   |

⚠️:所有版本均不可发布为与JetLinks同类的产品进行二次销售.
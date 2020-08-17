# JetLinks 物联网基础平台

![GitHub Workflow Status](https://img.shields.io/github/workflow/status/jetlinks/jetlinks-community/Auto%20Deploy%20Docker?label=docker)
![Version](https://img.shields.io/badge/Version-1.3--RELEASE-brightgreen)
![QQ群2021514](https://img.shields.io/badge/QQ群-2021514-brightgreen)
![jetlinks](https://visitor-badge.glitch.me/badge?page_id=jetlinks)

JetLinks 基于Java8,Spring Boot 2.x,WebFlux,Netty,Vert.x,Reactor等开发, 
是一个开箱即用,可二次开发的企业级物联网基础平台。平台实现了物联网相关的众多基础功能,
能帮助你快速建立物联网相关业务系统。
 

## 核心特性

支持统一物模型管理,多种设备,多种厂家,统一管理。

统一设备连接管理,多协议适配(TCP,MQTT,UDP,CoAP,HTTP等),屏蔽网络编程复杂性,灵活接入不同厂家不同协议的设备。

灵活的规则引擎,设备告警,消息通知,数据转发.可基于SQL进行复杂的数据处理逻辑.

地理位置:统一管理地理位置信息,支持区域搜索. 

官方QQ群: `2021514`

## 技术栈

1. [Spring Boot 2.2.x](https://spring.io/projects/spring-boot)
2. [Spring WebFlux](https://spring.io/) 响应式Web支持
3. [R2DBC](https://r2dbc.io/) 响应式关系型数据库驱动
4. [Project Reactor](https://projectreactor.io/) 响应式编程框架
4. [Netty](https://netty.io/) ,[Vert.x](https://vertx.io/) 高性能网络编程框架
5. [ElasticSearch](https://www.elastic.co/cn/products/enterprise-search) 全文检索，日志，时序数据存储
6. [PostgreSQL](https://www.postgresql.org) 业务功能数据管理
7. [hsweb framework 4](https://github.com/hs-web) 业务功能基础框架

## 架构

![platform](./platform.svg)

## 设备接入流程

![flow](./flow.svg)

## 模块

```bash
--jetlinks-community
------|----docker
------|------|----dev-env       # 启动开发环境
------|------|----run-all       # 启动全部,通过http://localhost:9000 访问系统.
------|----jetlinks-components  # 公共组件模块
------|----jetlinks-manager     # 业务管理模块
------|----jetlinks-standalone  # 服务启动模块
------|----simulator            # 设备模拟器
```

## 文档

[快速开始](http://doc.jetlinks.cn/basics-guide/quick-start.html) 
[开发文档](http://doc.jetlinks.cn/dev-guide/start.html) 
[常见问题](http://doc.jetlinks.cn/common-problems/network-components.html) 

## 许可版本

|  功能  |  社区版   | 专业版  |   企业版  |
| ----   |  ----  |   ----    |   -----   |
| 开放源代码      |  ✅ | ✅ |       ✅     |
| 设备管理,设备接入|  ✅ | ✅ |       ✅     |
| 多消息协议支持|  ✅ | ✅ |       ✅     |
| 规则引擎-设备告警        |  ✅ |  ✅ |     ✅     |
| 规则引擎-数据转发 |  ✅  |  ✅ |     ✅ |
| 系统监控,数据统计  |  ✅  |  ✅ |  ✅   |
| 邮件消息通知    |  ✅  |  ✅ |     ✅      |
| 微信企业消息    |  ✅  |  ✅ |     ✅      |
| 钉钉消息通知    |  ✅  |  ✅ |     ✅      |
| MQTT(TLS)    |  ✅  |  ✅ |   ✅   |
| TCP(TLS)     |  ✅  |  ✅ |  ✅    |
| CoAP(DTLS)    |  ⭕  |  ✅ |     ✅       |
| Http,WebSocket(TLS) |  ⭕  |  ✅ |     ✅ |
| 数据转发:MQTT,HTTP,Kafka... |  ⭕  |  ✅ |     ✅ |
| Geo地理位置支持     | ⭕   |  ✅ |  ✅    |
| 规则引擎-可视化设计器     | ⭕   |  ✅ |  ✅    |
| OpenAPI    |  ⭕  |  ✅ |     ✅     |
| 多租户(建设中)   |  ⭕  |  ✅ |   ✅   |
| 集群支持    |  ⭕  |  ✅ |     ✅     |
| QQ群技术支持 |  ⭕  |  ✅ |   ✅   |
| 一对一技术支持 |  ⭕  |  ⭕ |   ✅   |
| 微服务架构(建设中)   |  ⭕  |  ⭕ |   ✅   |
| 统一认证(建设中)   |  ⭕  |  ⭕ |   ✅   |
| 选配业务模块(建设中)   |  ⭕  |  ⭕ |   ✅   |
| 定制开发   |  ⭕  |  ⭕ |   ✅   |
| 商业限制   |  无  |  单个项目 |   无   |
| 定价   |  免费  | 联系我们  |  联系我们   |

⚠️:所有版本均不可发布为与JetLinks同类的产品进行二次销售. 

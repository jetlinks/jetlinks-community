# JetLinks 物联网基础平台

![GitHub Workflow Status](https://img.shields.io/github/workflow/status/jetlinks/jetlinks-community/Auto%20Deploy%20Docker?label=docker)
![Version](https://img.shields.io/badge/version-1.8--RELEASE-brightgreen)
[![QQ①群2021514](https://img.shields.io/badge/QQ①群-2021514-brightgreen)](https://qm.qq.com/cgi-bin/qm/qr?k=LGf0OPQqvLGdJIZST3VTcypdVWhdfAOG&jump_from=webapi)
[![QQ②群324606263](https://img.shields.io/badge/QQ②群-324606263-brightgreen)](https://qm.qq.com/cgi-bin/qm/qr?k=IMas2cH-TNsYxUcY8lRbsXqPnA2sGHYQ&jump_from=webapi)
![jetlinks](https://visitor-badge.glitch.me/badge?page_id=jetlinks)

JetLinks 基于Java8,Spring Boot 2.x,WebFlux,Netty,Vert.x,Reactor等开发, 
是一个开箱即用,可二次开发的企业级物联网基础平台。平台实现了物联网相关的众多基础功能,
能帮助你快速建立物联网相关业务系统。
 

## 核心特性

支持统一物模型管理,多种设备,多种厂家,统一管理。

统一设备连接管理,多协议适配(TCP,MQTT,UDP,CoAP,HTTP等),屏蔽网络编程复杂性,灵活接入不同厂家不同协议的设备。

灵活的规则引擎,设备告警,消息通知,数据转发.

强大的ReactorQL引擎,使用SQL来处理实时数据.

地理位置:统一管理地理位置信息,支持区域搜索. 

官方QQ: ①群 [2021514](https://qm.qq.com/cgi-bin/qm/qr?k=LGf0OPQqvLGdJIZST3VTcypdVWhdfAOG&jump_from=webapi)
, ②群 [324606263](https://qm.qq.com/cgi-bin/qm/qr?k=IMas2cH-TNsYxUcY8lRbsXqPnA2sGHYQ&jump_from=webapi)

## 技术栈

1. [Spring Boot 2.3.x](https://spring.io/projects/spring-boot)
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

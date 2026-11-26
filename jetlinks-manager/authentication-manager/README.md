# 权限与用户管理

用户权限由用户维度、默认权限及菜单授权共同初始化；正常未授权用户可以没有功能权限。

菜单定义保存、修改、删除，以及菜单授权和用户保存中的关联变更，在事务提交成功后清理派生缓存，再刷新用户认证信息；事务回滚不发布权限变化通知。菜单定义创建后使菜单缓存失效。菜单赋权与清空授权使用相同的缓存失效链路，同时清理短期菜单授权快照，避免旧的空授权被重新写入用户认证缓存。未解析到受影响用户时，不扩大为全量缓存清理。

相关入口为 `DefaultMenuService`、`MenuGrantService`、`AuthorizationSettingDetailService`、`UserDetailService` 和 `UserAuthenticationEventPublisher`。底层认证初始化错误处理与 Redis 跨节点缓存广播由 hsweb 负责。

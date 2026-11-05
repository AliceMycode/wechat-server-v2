# wechat-server-v2

IM 服务端 v2，在 `wechat-server` 的基础上重新设计库表与架构。

## 与 v1 的关系

v1（`wechat-server`）复刻自 easychat，沿用原有表结构并保持兼容，已停止功能扩展。
v2 是独立重写，**不与 v1 共享代码与库表**。

## 主要改进方向

- 消息撤回 / 删除 / 引用
- 朋友圈、收藏夹
- 文件走阿里云 OSS + CDN
- 服务端维护未读数，支持多端一致
- 离线消息基于消息序号（seq）游标，不再依赖 `last_off_time`
- 路由字段与会话字段分离（`receiveUserId` / `contactId`）

详细设计见 v1 仓库 `docs/design/新工程重设计要点.md`。

## 开发规范

遵循《阿里巴巴 Java 开发手册（黄山版）》。

## 目录结构

待补充。
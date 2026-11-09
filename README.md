# wechat-server-v2

IM 服务端 v2

## 当前阶段

第一阶段只做 IM 基础功能：注册登录、好友、单聊、群聊、文件、离线补拉、未读数。
消息撤回/删除/引用、已读回执、朋友圈/审核/三天可见、收藏、EMAS 移动推送放到后续迭代。

## 技术栈

JDK 21 + Spring Boot 4.1.x + MyBatis-Plus 3.5.18 + MySQL 8.0 + Redis 7 + Netty 4.2。
接口文档使用 Knife4j 4.5（OpenAPI 3，访问 `/doc.html`），底层由其内置的 springdoc-openapi 生成；Spring Boot 4 不使用旧的 Springfox 3.0。
依赖兼容性见 `docs/01-架构与技术栈.md` 的 4.3 节；Spring AI 2.x / spring-ai-agent-utils 后期接入。

## 当前部署

按 1～2 千同时在线验收：**1 台 4C8G 全栈**（应用 jar + MySQL 8 + Redis 7），文件走 OSS。
后续增长优先把 MySQL 拆到云 RDS，再按 8C16G / 多节点扩容。

## 数据库迁移

表结构由 **Flyway** 管理，迁移脚本放在 `src/main/resources/db/migration/`。
首版脚本：[V1__init_schema.sql](src/main/resources/db/migration/V1__init_schema.sql)。

### 命名规范

```text
V<版本号>__<描述>.sql        ← 版本号与描述之间是两个下划线
V2__create_message_receipt.sql   ✅
V2_create_message_receipt.sql    ❌ 只有一个下划线，Flyway 不识别
```

版本号用递增整数（V2、V3、V4…）；多人协作怕撞号时可改用日期序号：`V20261106_01__xxx.sql`。

### 变更操作规则

核心原则：**已执行过的脚本永远不改，任何变更都新增一个版本号更大的脚本。**

Flyway 启动时校验每个脚本的 checksum（记录在 `flyway_schema_history` 表），
修改已执行过的脚本会导致启动失败。常见变更全部通过追加脚本完成：

```sql
-- V2__create_message_receipt.sql：新增表
CREATE TABLE message_receipt (...);

-- V3__add_remark_to_friend.sql：加字段
ALTER TABLE friend ADD COLUMN remark varchar(64) NOT NULL DEFAULT '' COMMENT '好友备注';

-- V4__modify_remark_length.sql：改字段类型/长度
ALTER TABLE friend MODIFY COLUMN remark varchar(128) NOT NULL DEFAULT '' COMMENT '好友备注';

-- V5__drop_remark.sql：删字段
ALTER TABLE friend DROP COLUMN remark;

-- V6__drop_message_receipt.sql：删表（不可逆，线上执行前先备份）
DROP TABLE IF EXISTS message_receipt;
```

写好脚本 → 重启应用 → Flyway 自动执行未应用的新版本。

开发期刚执行的脚本写错了（未提交、仅本机库），可以在本机修正数据后
删除 `flyway_schema_history` 中对应版本行，重启重跑；
**此方法严禁用于已提交到 git 或已上生产的脚本**——生产只能追加新版本修正。

其他约定：

- 不要手工改生产库表结构，也不要用 `ddl-auto=update`；
- `flyway_schema_history` 是 Flyway 的账本，不要手工编辑它（上一段的本机救急除外）；
- 生产库执行删字段/删表前，先备份相关数据。

## 基础功能（第一阶段）

- 手机号注册 / 登录（阿里云 Dypnsapi）
- 好友、好友申请、拉黑、删除
- 单聊、群聊、建群/踢人/退群/解散
- 文件上传下载（OSS + CDN）
- 多端在线、离线消息补拉、未读数

## 后续迭代

- 消息撤回 / 删除 / 引用
- 已读回执与群已读详情
- 好友备注修改
- 朋友圈、收藏夹、审核、三天/半年可见
- 移动推送（EMAS）

详细设计见：

- [架构与技术栈设计（评审稿）](docs/01-架构与技术栈.md)
- [表结构说明（V1 基础 IM）](docs/02-表结构说明.md)
- v1 仓库 `docs/design/新工程重设计要点.md`

## 开发规范

遵循《阿里巴巴 Java 开发手册（黄山版）》。

代码格式由 [.editorconfig](.editorconfig) 统一：UTF-8、LF 换行、行尾不留空格、缩进统一 2 个空格、禁止 Tab；单行不超过 120 字符，Java 续行相对首行缩进 2 个空格（阿里规范为 4 个空格，本项目按 2 空格统一）。IntelliJ IDEA 默认支持 EditorConfig，无需额外插件；本地已开启保存时自动执行 Reformat code、Optimize imports、删除行尾空格、文件末尾补换行。

## 快速开始

1. 启动 MySQL 8 和 Redis。`wechat_v2` 库不需要手工创建，
   数据源 URL 带 `createDatabaseIfNotExist=true`，启动应用时由 JDBC 驱动自动建库。
2. 按需配置环境变量：`DB_URL`、`DB_USERNAME`、`DB_PASSWORD`、`REDIS_HOST`、`REDIS_PORT`、`REDIS_PASSWORD`；
3. 启动应用，Flyway 会自动执行 `db/migration` 下的脚本：

```bash
mvn spring-boot:run
```

默认 HTTP 端口 `8080`，健康检查 `GET /actuator/health`，接口文档 `GET /doc.html`。

## 目录结构

```text
pom.xml
mvnw / mvnw.cmd
src/main/java/com/wechat/www
├── WechatServerV2Application.java
└── config/                      # 基础配置（MyBatis-Plus、OpenAPI 等）
src/main/resources
├── application.yml
├── logback-spring.xml           # 日志输出格式与滚动策略
└── db/
    └── migration/               # Flyway 迁移脚本
src/test
└── resources/application.yml    # 测试使用 H2，关闭 Flyway
docs/
├── 01-架构与技术栈.md
└── 02-表结构说明.md
```

DDL 以 `src/main/resources/db/migration/` 下的 Flyway 脚本为准。

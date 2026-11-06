-- ============================================================
-- wechat-server-v2 基础 IM 表结构
-- Flyway V1 / MySQL 8.0 / InnoDB / utf8mb4
--
-- 约定：
-- 1. 不使用外键、触发器、存储过程；
-- 2. 内容表用 status/is_deleted 做逻辑删除，关系表用 status 表达业务状态；
-- 3. 时间统一 datetime(3)，精确到毫秒；
-- 4. 用户、会话、消息、文件主键使用雪花 ID，对前端输出时转字符串。
-- ============================================================

SET NAMES utf8mb4;

-- ------------------------------------------------------------
-- 账号
-- ------------------------------------------------------------
CREATE TABLE user_account (
  id              BIGINT UNSIGNED NOT NULL COMMENT '用户ID（雪花ID）',
  country_code    VARCHAR(8)      NOT NULL DEFAULT '86' COMMENT '国家区号',
  phone           VARCHAR(20)     NOT NULL COMMENT '手机号',
  password_hash   VARCHAR(100)    NULL COMMENT '密码哈希；短信登录可为空',
  status          TINYINT UNSIGNED NOT NULL DEFAULT 0 COMMENT '状态 0正常 1禁用 2注销',
  last_login_time DATETIME(3)     NULL COMMENT '最后登录时间',
  create_time     DATETIME(3)     NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '创建时间',
  update_time     DATETIME(3)     NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3) COMMENT '更新时间',
  is_deleted      TINYINT UNSIGNED NOT NULL DEFAULT 0 COMMENT '是否删除 1是 0否',
  PRIMARY KEY (id),
  UNIQUE KEY uk_country_phone (country_code, phone),
  KEY idx_status_create (status, create_time)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '用户账号';

CREATE TABLE user_profile (
  id              BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '自增ID',
  user_id         BIGINT UNSIGNED NOT NULL COMMENT '用户ID',
  nickname        VARCHAR(32)     NOT NULL DEFAULT '' COMMENT '昵称',
  avatar_file_id  BIGINT UNSIGNED NULL COMMENT '头像文件ID',
  signature       VARCHAR(128)    NOT NULL DEFAULT '' COMMENT '个性签名',
  gender          TINYINT UNSIGNED NOT NULL DEFAULT 0 COMMENT '性别 0未知 1男 2女',
  area            VARCHAR(64)     NOT NULL DEFAULT '' COMMENT '地区',
  birthday        DATE            NULL COMMENT '生日',
  create_time     DATETIME(3)     NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '创建时间',
  update_time     DATETIME(3)     NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3) COMMENT '更新时间',
  is_deleted      TINYINT UNSIGNED NOT NULL DEFAULT 0 COMMENT '是否删除 1是 0否',
  PRIMARY KEY (id),
  UNIQUE KEY uk_user_id (user_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '用户资料';

CREATE TABLE user_setting (
  id                      BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '自增ID',
  user_id                 BIGINT UNSIGNED NOT NULL COMMENT '用户ID',
  is_read_receipt_enabled TINYINT UNSIGNED NOT NULL DEFAULT 1 COMMENT '发送已读回执 1是 0否',
  is_push_enabled         TINYINT UNSIGNED NOT NULL DEFAULT 1 COMMENT '允许推送 1是 0否',
  is_friend_apply_need_verify TINYINT UNSIGNED NOT NULL DEFAULT 1 COMMENT '加好友需要验证 1是 0否',
  is_stranger_message_enabled TINYINT UNSIGNED NOT NULL DEFAULT 0 COMMENT '允许陌生人消息 1是 0否',
  create_time             DATETIME(3)     NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '创建时间',
  update_time             DATETIME(3)     NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3) COMMENT '更新时间',
  is_deleted              TINYINT UNSIGNED NOT NULL DEFAULT 0 COMMENT '是否删除 1是 0否',
  PRIMARY KEY (id),
  UNIQUE KEY uk_user_id (user_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '用户设置';

CREATE TABLE user_device (
  id              BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '自增ID',
  user_id         BIGINT UNSIGNED NOT NULL COMMENT '用户ID',
  device_id       VARCHAR(64)     NOT NULL COMMENT '设备唯一标识',
  device_type     TINYINT UNSIGNED NOT NULL DEFAULT 0 COMMENT '设备类型 1Electron 2iOS 3Android 4Web',
  device_name     VARCHAR(64)     NOT NULL DEFAULT '' COMMENT '设备名称',
  push_token      VARCHAR(256)    NULL COMMENT '推送token',
  push_provider   TINYINT UNSIGNED NOT NULL DEFAULT 0 COMMENT '推送通道 0无 1APNs 2华为 3小米 4OPPO 5vivo 6荣耀 7EMAS',
  last_active_time DATETIME(3)    NULL COMMENT '最后活跃时间',
  status          TINYINT UNSIGNED NOT NULL DEFAULT 0 COMMENT '状态 0正常 1禁用',
  create_time     DATETIME(3)     NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '创建时间',
  update_time     DATETIME(3)     NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3) COMMENT '更新时间',
  is_deleted      TINYINT UNSIGNED NOT NULL DEFAULT 0 COMMENT '是否删除 1是 0否',
  PRIMARY KEY (id),
  UNIQUE KEY uk_user_device (user_id, device_id),
  KEY idx_user_active (user_id, last_active_time)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '用户设备';

-- ------------------------------------------------------------
-- 好友关系
-- ------------------------------------------------------------
CREATE TABLE user_friend (
  id              BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '自增ID',
  user_id         BIGINT UNSIGNED NOT NULL COMMENT '用户ID',
  friend_user_id  BIGINT UNSIGNED NOT NULL COMMENT '好友用户ID',
  remark          VARCHAR(32)     NOT NULL DEFAULT '' COMMENT '好友备注',
  source          TINYINT UNSIGNED NOT NULL DEFAULT 0 COMMENT '来源 0搜索 1群聊 2二维码',
  status          TINYINT UNSIGNED NOT NULL DEFAULT 0 COMMENT '状态 0正常 1已删除',
  create_time     DATETIME(3)     NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '创建时间',
  update_time     DATETIME(3)     NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3) COMMENT '更新时间',
  PRIMARY KEY (id),
  UNIQUE KEY uk_user_friend (user_id, friend_user_id),
  KEY idx_friend_user (friend_user_id, status)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '好友关系（双向两行）';

CREATE TABLE friend_apply (
  id              BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '自增ID',
  apply_user_id   BIGINT UNSIGNED NOT NULL COMMENT '申请人ID',
  receive_user_id BIGINT UNSIGNED NOT NULL COMMENT '接收人ID',
  apply_message   VARCHAR(128)    NOT NULL DEFAULT '' COMMENT '申请留言',
  status          TINYINT UNSIGNED NOT NULL DEFAULT 0 COMMENT '状态 0待处理 1已同意 2已拒绝 3已过期',
  handle_time     DATETIME(3)     NULL COMMENT '处理时间',
  create_time     DATETIME(3)     NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '创建时间',
  update_time     DATETIME(3)     NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3) COMMENT '更新时间',
  PRIMARY KEY (id),
  KEY idx_receive_status (receive_user_id, status, create_time),
  KEY idx_apply_user (apply_user_id, create_time)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '好友申请';

CREATE TABLE user_blacklist (
  id              BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '自增ID',
  user_id         BIGINT UNSIGNED NOT NULL COMMENT '用户ID',
  blocked_user_id BIGINT UNSIGNED NOT NULL COMMENT '被拉黑用户ID',
  status          TINYINT UNSIGNED NOT NULL DEFAULT 0 COMMENT '状态 0拉黑中 1已解除',
  create_time     DATETIME(3)     NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '创建时间',
  update_time     DATETIME(3)     NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3) COMMENT '更新时间',
  PRIMARY KEY (id),
  UNIQUE KEY uk_user_blocked (user_id, blocked_user_id),
  KEY idx_blocked_user (blocked_user_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '用户黑名单';

-- ------------------------------------------------------------
-- 群
-- ------------------------------------------------------------
CREATE TABLE group_info (
  id              BIGINT UNSIGNED NOT NULL COMMENT '群ID（雪花ID）',
  group_no        VARCHAR(32)     NOT NULL COMMENT '群号',
  name            VARCHAR(64)     NOT NULL DEFAULT '' COMMENT '群名称',
  avatar_file_id  BIGINT UNSIGNED NULL COMMENT '群头像文件ID',
  owner_id        BIGINT UNSIGNED NOT NULL COMMENT '群主ID',
  member_count    INT UNSIGNED    NOT NULL DEFAULT 0 COMMENT '成员数',
  max_member      INT UNSIGNED    NOT NULL DEFAULT 500 COMMENT '最大成员数',
  join_type       TINYINT UNSIGNED NOT NULL DEFAULT 0 COMMENT '入群方式 0直接加入 1需要审批',
  notice          VARCHAR(500)    NOT NULL DEFAULT '' COMMENT '群公告',
  status          TINYINT UNSIGNED NOT NULL DEFAULT 0 COMMENT '状态 0正常 1已解散',
  create_time     DATETIME(3)     NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '创建时间',
  update_time     DATETIME(3)     NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3) COMMENT '更新时间',
  PRIMARY KEY (id),
  UNIQUE KEY uk_group_no (group_no),
  KEY idx_owner (owner_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '群信息';

CREATE TABLE group_member (
  id              BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '自增ID',
  group_id        BIGINT UNSIGNED NOT NULL COMMENT '群ID',
  user_id         BIGINT UNSIGNED NOT NULL COMMENT '用户ID',
  role            TINYINT UNSIGNED NOT NULL DEFAULT 0 COMMENT '角色 0成员 1管理员 2群主',
  group_nickname  VARCHAR(32)     NOT NULL DEFAULT '' COMMENT '群昵称',
  mute_until      DATETIME(3)     NULL COMMENT '禁言截止时间',
  status          TINYINT UNSIGNED NOT NULL DEFAULT 0 COMMENT '状态 0正常 1已退群 2被踢',
  join_time       DATETIME(3)     NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '入群时间',
  create_time     DATETIME(3)     NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '创建时间',
  update_time     DATETIME(3)     NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3) COMMENT '更新时间',
  PRIMARY KEY (id),
  UNIQUE KEY uk_group_user (group_id, user_id),
  KEY idx_user_status (user_id, status),
  KEY idx_group_status (group_id, status)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '群成员';

CREATE TABLE group_apply (
  id              BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '自增ID',
  group_id        BIGINT UNSIGNED NOT NULL COMMENT '群ID',
  apply_user_id   BIGINT UNSIGNED NOT NULL COMMENT '申请人ID',
  inviter_id      BIGINT UNSIGNED NULL COMMENT '邀请人ID',
  apply_message   VARCHAR(128)    NOT NULL DEFAULT '' COMMENT '申请留言',
  status          TINYINT UNSIGNED NOT NULL DEFAULT 0 COMMENT '状态 0待处理 1已同意 2已拒绝 3已过期',
  handle_user_id  BIGINT UNSIGNED NULL COMMENT '处理人ID',
  handle_time     DATETIME(3)     NULL COMMENT '处理时间',
  create_time     DATETIME(3)     NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '创建时间',
  update_time     DATETIME(3)     NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3) COMMENT '更新时间',
  PRIMARY KEY (id),
  KEY idx_group_status (group_id, status, create_time),
  KEY idx_apply_user (apply_user_id, create_time)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '入群申请';

-- ------------------------------------------------------------
-- 会话与消息
-- ------------------------------------------------------------
CREATE TABLE conversation (
  id                  BIGINT UNSIGNED NOT NULL COMMENT '会话ID（雪花ID）',
  type                TINYINT UNSIGNED NOT NULL COMMENT '类型 1单聊 2群聊',
  single_key          VARCHAR(64)     NULL COMMENT '单聊唯一键：minUserId:maxUserId',
  group_id            BIGINT UNSIGNED NULL COMMENT '群ID，单聊为空',
  max_seq             BIGINT UNSIGNED NOT NULL DEFAULT 0 COMMENT '会话内最大消息序号',
  last_message_id     BIGINT UNSIGNED NULL COMMENT '最后一条消息ID',
  last_message_preview VARCHAR(255)   NOT NULL DEFAULT '' COMMENT '最后一条消息摘要',
  last_message_time   DATETIME(3)     NULL COMMENT '最后一条消息时间',
  status              TINYINT UNSIGNED NOT NULL DEFAULT 0 COMMENT '状态 0正常 1关闭',
  create_time         DATETIME(3)     NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '创建时间',
  update_time         DATETIME(3)     NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3) COMMENT '更新时间',
  PRIMARY KEY (id),
  UNIQUE KEY uk_single_key (single_key),
  UNIQUE KEY uk_group_id (group_id),
  KEY idx_last_message_time (last_message_time)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '会话';

CREATE TABLE conversation_member (
  id                  BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '自增ID',
  conversation_id     BIGINT UNSIGNED NOT NULL COMMENT '会话ID',
  user_id             BIGINT UNSIGNED NOT NULL COMMENT '用户ID',
  last_read_seq       BIGINT UNSIGNED NOT NULL DEFAULT 0 COMMENT '已读到的消息序号',
  last_delivered_seq  BIGINT UNSIGNED NOT NULL DEFAULT 0 COMMENT '已推送到客户端的消息序号',
  deleted_seq         BIGINT UNSIGNED NOT NULL DEFAULT 0 COMMENT '删除会话时的消息水位',
  is_pinned           TINYINT UNSIGNED NOT NULL DEFAULT 0 COMMENT '是否置顶 1是 0否',
  is_muted            TINYINT UNSIGNED NOT NULL DEFAULT 0 COMMENT '是否免打扰 1是 0否',
  join_time           DATETIME(3)     NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '加入会话时间',
  create_time         DATETIME(3)     NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '创建时间',
  update_time         DATETIME(3)     NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3) COMMENT '更新时间',
  PRIMARY KEY (id),
  UNIQUE KEY uk_conversation_user (conversation_id, user_id),
  KEY idx_user_update (user_id, update_time),
  KEY idx_conversation_read (conversation_id, last_read_seq)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '会话成员';

CREATE TABLE chat_message (
  id              BIGINT UNSIGNED NOT NULL COMMENT '消息ID（雪花ID）',
  conversation_id BIGINT UNSIGNED NOT NULL COMMENT '会话ID',
  seq             BIGINT UNSIGNED NOT NULL COMMENT '会话内单调递增序号',
  sender_id       BIGINT UNSIGNED NOT NULL COMMENT '发送人ID',
  message_type    TINYINT UNSIGNED NOT NULL COMMENT '类型 1文本 2图片 3视频 4文件 5语音 6系统',
  content         JSON            NULL COMMENT '按类型定义的结构化内容',
  file_id         BIGINT UNSIGNED NULL COMMENT '单文件消息的文件ID',
  client_msg_id   VARCHAR(64)     NULL COMMENT '客户端幂等键',
  status          TINYINT UNSIGNED NOT NULL DEFAULT 0 COMMENT '状态 0正常 1撤回 2管理员删除',
  send_time       DATETIME(3)     NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '发送时间',
  create_time     DATETIME(3)     NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '创建时间',
  update_time     DATETIME(3)     NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3) COMMENT '更新时间',
  PRIMARY KEY (id),
  UNIQUE KEY uk_conversation_seq (conversation_id, seq),
  UNIQUE KEY uk_sender_client (sender_id, client_msg_id),
  KEY idx_conversation_time (conversation_id, send_time),
  KEY idx_sender_time (sender_id, send_time)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '聊天消息';

CREATE TABLE message_mention (
  id              BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '自增ID',
  message_id      BIGINT UNSIGNED NOT NULL COMMENT '消息ID',
  conversation_id BIGINT UNSIGNED NOT NULL COMMENT '会话ID',
  user_id         BIGINT UNSIGNED NOT NULL COMMENT '被@用户ID',
  create_time     DATETIME(3)     NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '创建时间',
  update_time     DATETIME(3)     NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3) COMMENT '更新时间',
  PRIMARY KEY (id),
  UNIQUE KEY uk_message_user (message_id, user_id),
  KEY idx_user_create (user_id, create_time)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '消息@提醒';

CREATE TABLE event_outbox (
  id              BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '自增ID',
  event_type      VARCHAR(64)     NOT NULL COMMENT '事件类型',
  aggregate_id    BIGINT UNSIGNED NOT NULL COMMENT '聚合ID，如消息ID/会话ID',
  payload         JSON            NOT NULL COMMENT '事件内容',
  status          TINYINT UNSIGNED NOT NULL DEFAULT 0 COMMENT '状态 0待发 1已发 2失败',
  retry_count     INT UNSIGNED    NOT NULL DEFAULT 0 COMMENT '重试次数',
  next_retry_time DATETIME(3)     NULL COMMENT '下次重试时间',
  create_time     DATETIME(3)     NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '创建时间',
  update_time     DATETIME(3)     NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3) COMMENT '更新时间',
  PRIMARY KEY (id),
  KEY idx_status_next (status, next_retry_time),
  KEY idx_create_time (create_time)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '事件发件箱';

-- ------------------------------------------------------------
-- 文件与短信
-- ------------------------------------------------------------
CREATE TABLE file_info (
  id              BIGINT UNSIGNED NOT NULL COMMENT '文件ID（雪花ID）',
  uploader_id     BIGINT UNSIGNED NOT NULL COMMENT '上传人ID',
  biz_type        TINYINT UNSIGNED NOT NULL COMMENT '业务类型 1头像 2聊天 3朋友圈 4收藏',
  bucket          VARCHAR(64)     NOT NULL COMMENT 'OSS bucket',
  object_key      VARCHAR(255)    NOT NULL COMMENT 'OSS object key',
  original_name   VARCHAR(255)    NOT NULL DEFAULT '' COMMENT '原始文件名',
  content_type    VARCHAR(128)    NOT NULL DEFAULT '' COMMENT 'MIME类型',
  size_bytes      BIGINT UNSIGNED NOT NULL DEFAULT 0 COMMENT '文件大小',
  sha256          CHAR(64)        NULL COMMENT '文件SHA-256',
  width           INT UNSIGNED    NULL COMMENT '图片/视频宽度',
  height          INT UNSIGNED    NULL COMMENT '图片/视频高度',
  duration_ms     INT UNSIGNED    NULL COMMENT '音视频时长毫秒',
  cover_object_key VARCHAR(255)   NULL COMMENT '封面图object key',
  status          TINYINT UNSIGNED NOT NULL DEFAULT 0 COMMENT '状态 0上传中 1可用 2审核拒绝 3已删除',
  create_time     DATETIME(3)     NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '创建时间',
  update_time     DATETIME(3)     NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3) COMMENT '更新时间',
  PRIMARY KEY (id),
  UNIQUE KEY uk_object_key (object_key),
  KEY idx_uploader_create (uploader_id, create_time)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '文件信息';

CREATE TABLE sms_record (
  id              BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '自增ID',
  phone           VARCHAR(20)     NOT NULL COMMENT '手机号',
  scene           VARCHAR(32)     NOT NULL COMMENT '场景 register/login/reset',
  biz_id          VARCHAR(64)     NULL COMMENT '发送回执ID',
  request_id      VARCHAR(64)     NULL COMMENT '请求ID',
  send_status     TINYINT UNSIGNED NOT NULL DEFAULT 0 COMMENT '状态 0发送中 1成功 2失败',
  error_code      VARCHAR(64)     NULL COMMENT '错误码',
  ip              VARCHAR(45)     NULL COMMENT '请求IP',
  device_id       VARCHAR(64)     NULL COMMENT '设备标识',
  create_time     DATETIME(3)     NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '创建时间',
  update_time     DATETIME(3)     NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3) COMMENT '更新时间',
  PRIMARY KEY (id),
  KEY idx_phone_create (phone, create_time),
  KEY idx_ip_create (ip, create_time),
  KEY idx_request_id (request_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '短信发送记录';

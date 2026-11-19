-- 注册即设密码（对标微信注册流程：昵称 + 手机号 + 密码 → 验证码）。
-- password_hash 由可空收紧为非空：所有账号必须能密码登录。
--
-- 注意：存量库中若 user_account 已有 NULL 行，MySQL 严格模式会拒绝本 ALTER。
-- 开发期表内无数据可直接执行；生产前如有存量数据，须先 SELECT 确认并清洗再执行
-- （手册【强制】：数据订正前先 select，确认无误才能执行更新语句）。
ALTER TABLE user_account
  MODIFY COLUMN password_hash VARCHAR(100) NOT NULL COMMENT '密码哈希（BCrypt，自带随机盐，固定 60 字符）';

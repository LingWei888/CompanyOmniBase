-- 站长与普通用户分表：admin_user / sys_user（仅前台用户）
CREATE TABLE IF NOT EXISTS admin_user (
    id          BIGINT PRIMARY KEY AUTO_INCREMENT,
    username    VARCHAR(64)  NOT NULL UNIQUE,
    password    VARCHAR(128) NOT NULL,
    nickname    VARCHAR(64)  NULL,
    enabled     TINYINT(1)   NOT NULL DEFAULT 1,
    created_at  DATETIME     NOT NULL,
    updated_at  DATETIME     NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 迁移原 ADMIN 账号到站长表
INSERT INTO admin_user (username, password, nickname, enabled, created_at, updated_at)
SELECT username, password, nickname, enabled, created_at, updated_at
FROM sys_user
WHERE role = 'ADMIN'
  AND NOT EXISTS (SELECT 1 FROM admin_user a WHERE a.username = sys_user.username);

DELETE FROM sys_user WHERE role = 'ADMIN';

ALTER TABLE sys_user DROP COLUMN role;

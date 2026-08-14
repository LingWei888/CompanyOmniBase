-- Day2: 账号体系
CREATE TABLE IF NOT EXISTS sys_user (
    id          BIGINT PRIMARY KEY AUTO_INCREMENT,
    username    VARCHAR(64)  NOT NULL UNIQUE,
    password    VARCHAR(128) NOT NULL,
    nickname    VARCHAR(64)  NULL,
    role        VARCHAR(16)  NOT NULL,
    enabled     TINYINT(1)   NOT NULL DEFAULT 1,
    created_at  DATETIME     NOT NULL,
    updated_at  DATETIME     NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

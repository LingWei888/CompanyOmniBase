-- Day: 前台用户头像与套餐
ALTER TABLE sys_user
    ADD COLUMN avatar_url VARCHAR(512) NULL AFTER nickname,
    ADD COLUMN plan VARCHAR(32) NOT NULL DEFAULT 'FREE' AFTER role;

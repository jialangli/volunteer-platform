-- ============================================================
-- 志愿服务管理平台 · 数据库初始化脚本
-- MySQL 8.0+ / utf8mb4
-- 执行方式：mysql -u root -p < init.sql  或 在 Workbench 中整段执行
-- ============================================================

CREATE DATABASE IF NOT EXISTS volunteer DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE volunteer;

-- ---------- 用户 ----------
CREATE TABLE IF NOT EXISTS users (
  id            BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
  username      VARCHAR(50)  NOT NULL COMMENT '登录名',
  password_hash VARCHAR(100) NOT NULL COMMENT 'bcrypt 哈希',
  real_name     VARCHAR(50)  NOT NULL COMMENT '真实姓名',
  student_no    VARCHAR(30)  DEFAULT NULL COMMENT '学号',
  phone         VARCHAR(64)  DEFAULT NULL COMMENT '手机号（应用层 AES 加密存储）',
  avatar_url    VARCHAR(255) DEFAULT NULL,
  status        TINYINT      NOT NULL DEFAULT 1 COMMENT '1=正常 0=禁用',
  created_at    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  deleted_at    DATETIME     DEFAULT NULL COMMENT '软删除',
  KEY idx_users_created (created_at)
) ENGINE=InnoDB COMMENT='用户表';

-- ---------- 角色 ----------
CREATE TABLE IF NOT EXISTS roles (
  id         BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
  code       VARCHAR(30) NOT NULL UNIQUE COMMENT 'VOLUNTEER/ORGANIZER/ADMIN',
  name       VARCHAR(30) NOT NULL COMMENT '角色名',
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB COMMENT='角色表';

CREATE TABLE IF NOT EXISTS user_roles (
  user_id BIGINT UNSIGNED NOT NULL,
  role_id BIGINT UNSIGNED NOT NULL,
  PRIMARY KEY (user_id, role_id),
  KEY idx_user_roles_role (role_id),
  CONSTRAINT fk_ur_user FOREIGN KEY (user_id) REFERENCES users (id),
  CONSTRAINT fk_ur_role FOREIGN KEY (role_id) REFERENCES roles (id)
) ENGINE=InnoDB COMMENT='用户-角色关联表';

-- ---------- 活动类型 ----------
CREATE TABLE IF NOT EXISTS activity_type (
  id         BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
  name       VARCHAR(30) NOT NULL,
  status     TINYINT NOT NULL DEFAULT 1 COMMENT '1=启用 0=停用',
  sort       INT NOT NULL DEFAULT 0,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB COMMENT='活动类型表';

-- ---------- 活动 ----------
CREATE TABLE IF NOT EXISTS activity (
  id                  BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
  title               VARCHAR(100) NOT NULL COMMENT '活动标题',
  description         TEXT COMMENT '活动描述',
  type_id             BIGINT UNSIGNED DEFAULT NULL,
  organizer_id        BIGINT UNSIGNED NOT NULL COMMENT '组织者用户ID',
  location            VARCHAR(255) DEFAULT NULL,
  start_time          DATETIME NOT NULL COMMENT '活动开始时间',
  end_time            DATETIME NOT NULL COMMENT '活动结束时间',
  signin_start        DATETIME NOT NULL COMMENT '签到开放时间',
  signin_end          DATETIME NOT NULL COMMENT '签到截止时间',
  signout_end         DATETIME NOT NULL COMMENT '签退截止时间',
  capacity            INT NOT NULL DEFAULT 0 COMMENT '名额，0=不限',
  capacity_remaining  INT NOT NULL DEFAULT 0 COMMENT '剩余名额（防超卖原子扣减）',
  status              VARCHAR(20) NOT NULL DEFAULT 'DRAFT' COMMENT 'DRAFT/RECRUITING/ONGOING/FINISHED/ARCHIVED/CANCELLED',
  cover_url           VARCHAR(255) DEFAULT NULL,
  version             INT NOT NULL DEFAULT 0 COMMENT '乐观锁版本号',
  created_at          DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at          DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  deleted_at          DATETIME DEFAULT NULL,
  KEY idx_activity_status_time (status, start_time),
  KEY idx_activity_org (organizer_id),
  CONSTRAINT chk_activity_capacity CHECK (capacity >= 0 AND capacity_remaining >= 0)
) ENGINE=InnoDB COMMENT='活动表';

-- ---------- 报名 ----------
CREATE TABLE IF NOT EXISTS registration (
  id           BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
  activity_id  BIGINT UNSIGNED NOT NULL,
  user_id      BIGINT UNSIGNED NOT NULL,
  status       VARCHAR(20) NOT NULL DEFAULT 'PENDING' COMMENT 'PENDING/APPROVED/REJECTED/CANCELLED/WAIVED',
  apply_time   DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  audit_time   DATETIME DEFAULT NULL,
  audit_by     BIGINT UNSIGNED DEFAULT NULL,
  audit_reason VARCHAR(200) DEFAULT NULL,
  created_at   DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at   DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  UNIQUE KEY uk_reg_activity_user (activity_id, user_id),
  KEY idx_reg_activity_status (activity_id, status),
  KEY idx_reg_user (user_id),
  CONSTRAINT fk_reg_activity FOREIGN KEY (activity_id) REFERENCES activity (id),
  CONSTRAINT fk_reg_user FOREIGN KEY (user_id) REFERENCES users (id)
) ENGINE=InnoDB COMMENT='报名表（唯一约束防重复报名）';

-- ---------- 签到签退 ----------
CREATE TABLE IF NOT EXISTS attendance (
  id               BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
  registration_id  BIGINT UNSIGNED NOT NULL,
  signin_time      DATETIME DEFAULT NULL,
  signout_time     DATETIME DEFAULT NULL,
  duration_minutes INT NOT NULL DEFAULT 0 COMMENT '有效服务时长（分钟）',
  signin_method    VARCHAR(20) NOT NULL DEFAULT 'QR' COMMENT 'QR/GEO/MANUAL',
  audit_status     VARCHAR(20) NOT NULL DEFAULT 'VALID' COMMENT 'VALID/PENDING/INVALID',
  remark           VARCHAR(200) DEFAULT NULL,
  created_at       DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at       DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  UNIQUE KEY uk_att_registration (registration_id),
  CONSTRAINT fk_att_reg FOREIGN KEY (registration_id) REFERENCES registration (id)
) ENGINE=InnoDB COMMENT='签到记录表（一人一活动仅一条）';

-- ---------- 工时流水 ----------
CREATE TABLE IF NOT EXISTS service_hours (
  id                 BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
  user_id            BIGINT UNSIGNED NOT NULL,
  activity_id        BIGINT UNSIGNED NOT NULL,
  attendance_id      BIGINT UNSIGNED NOT NULL,
  hours              DECIMAL(6,2) NOT NULL COMMENT '核算时长（小时）',
  calc_rule_version  VARCHAR(20) NOT NULL COMMENT '核算规则版本号，保证口径可回溯',
  status             VARCHAR(20) NOT NULL DEFAULT 'CONFIRMED' COMMENT 'CONFIRMED/PENDING/CANCELLED',
  created_at         DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  KEY idx_hours_user (user_id, created_at),
  KEY idx_hours_activity (activity_id),
  UNIQUE KEY uk_hours_attendance (attendance_id),
  CONSTRAINT fk_hours_user FOREIGN KEY (user_id) REFERENCES users (id),
  CONSTRAINT fk_hours_activity FOREIGN KEY (activity_id) REFERENCES activity (id)
) ENGINE=InnoDB COMMENT='工时流水表';

-- ---------- 服务证明 ----------
CREATE TABLE IF NOT EXISTS certification (
  id           BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
  user_id      BIGINT UNSIGNED NOT NULL,
  period_label VARCHAR(30) NOT NULL COMMENT '证明周期，如 2025-2026 学年',
  total_hours  DECIMAL(8,2) NOT NULL COMMENT '周期内累计工时',
  serial_no    VARCHAR(40) NOT NULL UNIQUE COMMENT '证明编号（防伪）',
  status       VARCHAR(20) NOT NULL DEFAULT 'GENERATED',
  pdf_url      VARCHAR(255) DEFAULT NULL,
  created_at   DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  KEY idx_cert_user (user_id),
  CONSTRAINT fk_cert_user FOREIGN KEY (user_id) REFERENCES users (id)
) ENGINE=InnoDB COMMENT='服务证明表';

-- ---------- 公告 ----------
CREATE TABLE IF NOT EXISTS announcement (
  id           BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
  title        VARCHAR(100) NOT NULL,
  content      TEXT,
  is_top       TINYINT NOT NULL DEFAULT 0,
  status       TINYINT NOT NULL DEFAULT 1 COMMENT '1=发布 0=下架',
  publish_time DATETIME DEFAULT NULL,
  created_at   DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at   DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB COMMENT='公告表';

-- ---------- 操作审计日志 ----------
CREATE TABLE IF NOT EXISTS operation_log (
  id          BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
  user_id     BIGINT UNSIGNED DEFAULT NULL,
  module      VARCHAR(30) NOT NULL COMMENT '模块：auth/activity/registration/...',
  action      VARCHAR(50) NOT NULL COMMENT '动作：LOGIN/CREATE/AUDIT/CHECKIN/...',
  target_type VARCHAR(30) DEFAULT NULL,
  target_id   BIGINT UNSIGNED DEFAULT NULL,
  detail      JSON DEFAULT NULL COMMENT '变更前后快照',
  ip          VARCHAR(45) DEFAULT NULL,
  created_at  DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  KEY idx_log_user_time (user_id, created_at),
  KEY idx_log_module (module)
) ENGINE=InnoDB COMMENT='操作审计日志表';

-- ============================================================
-- 初始数据（种子）
-- ============================================================

INSERT INTO roles (code, name) VALUES
  ('VOLUNTEER', '志愿者'),
  ('ORGANIZER', '组织者'),
  ('ADMIN',     '管理员');

INSERT INTO activity_type (name, sort) VALUES
  ('社区服务', 1),
  ('支教助学', 2),
  ('环保公益', 3),
  ('大型赛会', 4),
  ('敬老助残', 5),
  ('其他',     6);

INSERT INTO announcement (title, content, is_top, status, publish_time) VALUES
  ('欢迎使用志愿服务管理平台', '本平台用于活动发布、报名、签到签退与工时记录，所有服务数据将作为评奖评优的依据。', 1, 1, NOW());

-- 说明：初始管理员账号由后端 DataInitializer 在认证模块上线时自动创建（admin / admin123，首次登录后请修改密码）。

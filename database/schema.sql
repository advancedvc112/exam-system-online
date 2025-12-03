-- ============================================
-- 高并发在线考试系统 - 数据库表结构
-- 数据库名: exam_online
-- ============================================

-- 创建数据库（如果不存在）
CREATE DATABASE IF NOT EXISTS `exam_online` DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

USE `exam_online`;

-- ============================================
-- 1. 用户表 (user)
-- ============================================
DROP TABLE IF EXISTS `user`;
CREATE TABLE `user` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '用户ID',
    `username` VARCHAR(50) NOT NULL COMMENT '用户名',
    `password` VARCHAR(255) NOT NULL COMMENT '密码（MD5加密后）',
    `role` VARCHAR(20) NOT NULL DEFAULT 'student' COMMENT '用户角色：admin-管理员, teacher-教师, student-学生',
    `status` TINYINT NOT NULL DEFAULT 1 COMMENT '用户状态：0-禁用, 1-启用',
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_username` (`username`),
    KEY `idx_role` (`role`),
    KEY `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户表';

-- ============================================
-- 2. 题目表 (question)
-- ============================================
DROP TABLE IF EXISTS `question`;
CREATE TABLE `question` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '题目ID',
    `content` TEXT NOT NULL COMMENT '题目内容',
    `type` VARCHAR(20) NOT NULL COMMENT '题目类型：single_choice-单选, multiple_choice-多选, true_false-判断, fill_blank-填空, short_answer-简答',
    `difficulty` VARCHAR(10) NOT NULL DEFAULT 'medium' COMMENT '题目难度：easy-简单, medium-中等, hard-困难',
    `options` TEXT COMMENT '选项（JSON格式，用于单选和多选）',
    `answer` TEXT NOT NULL COMMENT '正确答案',
    `score` INT NOT NULL DEFAULT 5 COMMENT '分值',
    `category` VARCHAR(100) COMMENT '题目分类/知识点',
    `create_user_id` BIGINT COMMENT '创建人ID',
    `status` TINYINT NOT NULL DEFAULT 1 COMMENT '状态：0-禁用, 1-启用',
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    KEY `idx_type` (`type`),
    KEY `idx_difficulty` (`difficulty`),
    KEY `idx_category` (`category`),
    KEY `idx_status` (`status`),
    KEY `idx_create_user_id` (`create_user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='题目表';

-- ============================================
-- 插入测试数据
-- ============================================
-- 注意：密码是 '123456' 的 MD5 值：e10adc3949ba59abbe56e057f20f883e
INSERT INTO `user` (`username`, `password`, `role`, `status`) VALUES
('admin', 'e10adc3949ba59abbe56e057f20f883e', 'admin', 1),
('teacher1', 'e10adc3949ba59abbe56e057f20f883e', 'teacher', 1),
('student1', 'e10adc3949ba59abbe56e057f20f883e', 'student', 1);

-- 插入测试题目数据
INSERT INTO `question` (`content`, `type`, `difficulty`, `options`, `answer`, `score`, `category`, `create_user_id`, `status`) VALUES
('Java中哪个关键字用于继承？', 'single_choice', 'easy', '{"A":"extends","B":"implements","C":"inherit","D":"super"}', 'A', 5, 'Java基础', 1, 1),
('以下哪些是Java的基本数据类型？（多选）', 'multiple_choice', 'medium', '{"A":"int","B":"String","C":"double","D":"boolean"}', 'A,C,D', 10, 'Java基础', 1, 1),
('Java中String是不可变的。', 'true_false', 'easy', NULL, 'true', 3, 'Java基础', 1, 1),
('Java中用于创建对象的关键字是______。', 'fill_blank', 'easy', NULL, 'new', 5, 'Java基础', 1, 1),
('请简述Java中抽象类和接口的区别。', 'short_answer', 'hard', NULL, '抽象类可以有构造方法，接口不能；抽象类可以有普通方法，接口只能有抽象方法（Java8之前）；一个类只能继承一个抽象类，但可以实现多个接口。', 15, 'Java面向对象', 1, 1);

-- ============================================
-- 3. 试卷表 (paper)
-- ============================================
DROP TABLE IF EXISTS `paper`;
CREATE TABLE `paper` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '试卷ID',
    `name` VARCHAR(200) NOT NULL COMMENT '试卷名称',
    `description` TEXT COMMENT '试卷描述',
    `type` VARCHAR(20) NOT NULL COMMENT '试卷类型：random-随机组卷, fixed-固定组卷',
    `total_score` INT NOT NULL COMMENT '总分数',
    `duration` INT NOT NULL COMMENT '考试时长（分钟）',
    `create_user_id` BIGINT COMMENT '创建人ID',
    `status` TINYINT NOT NULL DEFAULT 1 COMMENT '状态：0-禁用, 1-启用',
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    KEY `idx_type` (`type`),
    KEY `idx_status` (`status`),
    KEY `idx_create_user_id` (`create_user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='试卷表';

-- ============================================
-- 4. 试卷题目关联表 (paper_question)
-- ============================================
DROP TABLE IF EXISTS `paper_question`;
CREATE TABLE `paper_question` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '关联ID',
    `paper_id` BIGINT NOT NULL COMMENT '试卷ID',
    `question_id` BIGINT NOT NULL COMMENT '题目ID',
    `order_num` INT NOT NULL COMMENT '题目序号（在试卷中的顺序）',
    `score` INT NOT NULL COMMENT '该题在试卷中的分值',
    PRIMARY KEY (`id`),
    KEY `idx_paper_id` (`paper_id`),
    KEY `idx_question_id` (`question_id`),
    UNIQUE KEY `uk_paper_question` (`paper_id`, `question_id`),
    CONSTRAINT `fk_paper_question_paper` FOREIGN KEY (`paper_id`) REFERENCES `paper` (`id`) ON DELETE CASCADE,
    CONSTRAINT `fk_paper_question_question` FOREIGN KEY (`question_id`) REFERENCES `question` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='试卷题目关联表';

-- ============================================
-- 5. 考试安排表 (exam)
-- ============================================
DROP TABLE IF EXISTS `exam`;
CREATE TABLE `exam` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '考试ID',
    `name` VARCHAR(200) NOT NULL COMMENT '考试名称',
    `description` TEXT COMMENT '考试描述',
    `paper_id` BIGINT NOT NULL COMMENT '试卷ID',
    `start_time` DATETIME NOT NULL COMMENT '开始时间',
    `end_time` DATETIME NOT NULL COMMENT '结束时间',
    `duration` INT NOT NULL COMMENT '考试时长（分钟）',
    `status` VARCHAR(20) NOT NULL DEFAULT 'not_started' COMMENT '考试状态：not_started-未开始, in_progress-进行中, finished-已结束, cancelled-已取消',
    `create_user_id` BIGINT COMMENT '创建人ID（教师/管理员）',
    `allow_view_answer` TINYINT NOT NULL DEFAULT 0 COMMENT '是否允许查看答案：0-不允许, 1-允许',
    `allow_retake` TINYINT NOT NULL DEFAULT 0 COMMENT '是否允许补考：0-不允许, 1-允许',
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    KEY `idx_paper_id` (`paper_id`),
    KEY `idx_status` (`status`),
    KEY `idx_start_time` (`start_time`),
    KEY `idx_end_time` (`end_time`),
    KEY `idx_create_user_id` (`create_user_id`),
    CONSTRAINT `fk_exam_paper` FOREIGN KEY (`paper_id`) REFERENCES `paper` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='考试安排表';

-- ============================================
-- 6. 考试记录表 (exam_record)
-- ============================================
DROP TABLE IF EXISTS `exam_record`;
CREATE TABLE `exam_record` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '记录ID',
    `exam_id` BIGINT NOT NULL COMMENT '考试ID',
    `student_id` BIGINT NOT NULL COMMENT '学生ID',
    `paper_id` BIGINT NOT NULL COMMENT '试卷ID',
    `start_time` DATETIME NOT NULL COMMENT '开始答题时间',
    `submit_time` DATETIME COMMENT '提交时间',
    `total_score` INT COMMENT '总分数',
    `score` INT COMMENT '得分',
    `status` VARCHAR(20) NOT NULL DEFAULT 'in_progress' COMMENT '状态：in_progress-答题中, submitted-已提交, timeout-超时, cancelled-取消',
    `switch_count` INT NOT NULL DEFAULT 0 COMMENT '切屏次数',
    `is_cheating` TINYINT NOT NULL DEFAULT 0 COMMENT '是否作弊：0-否, 1-是',
    `cheating_reason` VARCHAR(500) COMMENT '作弊原因',
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    KEY `idx_exam_id` (`exam_id`),
    KEY `idx_student_id` (`student_id`),
    KEY `idx_paper_id` (`paper_id`),
    KEY `idx_status` (`status`),
    UNIQUE KEY `uk_exam_student` (`exam_id`, `student_id`),
    CONSTRAINT `fk_exam_record_exam` FOREIGN KEY (`exam_id`) REFERENCES `exam` (`id`),
    CONSTRAINT `fk_exam_record_student` FOREIGN KEY (`student_id`) REFERENCES `user` (`id`),
    CONSTRAINT `fk_exam_record_paper` FOREIGN KEY (`paper_id`) REFERENCES `paper` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='考试记录表';

-- ============================================
-- 7. 答题记录表 (answer_record)
-- ============================================
DROP TABLE IF EXISTS `answer_record`;
CREATE TABLE `answer_record` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '记录ID',
    `exam_record_id` BIGINT NOT NULL COMMENT '考试记录ID',
    `question_id` BIGINT NOT NULL COMMENT '题目ID',
    `student_answer` TEXT COMMENT '学生答案',
    `is_correct` TINYINT COMMENT '是否正确：0-错误, 1-正确, 2-部分正确（多选题）',
    `score` INT COMMENT '得分',
    `answer_time` BIGINT COMMENT '答题时间（毫秒）',
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    KEY `idx_exam_record_id` (`exam_record_id`),
    KEY `idx_question_id` (`question_id`),
    UNIQUE KEY `uk_exam_record_question` (`exam_record_id`, `question_id`),
    CONSTRAINT `fk_answer_record_exam_record` FOREIGN KEY (`exam_record_id`) REFERENCES `exam_record` (`id`) ON DELETE CASCADE,
    CONSTRAINT `fk_answer_record_question` FOREIGN KEY (`question_id`) REFERENCES `question` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='答题记录表';


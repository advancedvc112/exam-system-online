package com.exam.manage.dal.dataobject;

import lombok.Data;
import java.time.LocalDateTime;

/**
 * 考试安排实体类
 */
@Data
public class ExamDO {
    /**
     * 考试ID
     */
    private Long id;

    /**
     * 考试名称
     */
    private String name;

    /**
     * 考试描述
     */
    private String description;

    /**
     * 试卷ID
     */
    private Long paperId;

    /**
     * 开始时间
     */
    private LocalDateTime startTime;

    /**
     * 结束时间
     */
    private LocalDateTime endTime;

    /**
     * 考试时长（分钟）
     */
    private Integer duration;

    /**
     * 考试状态：not_started-未开始, in_progress-进行中, finished-已结束, cancelled-已取消
     */
    private String status;

    /**
     * 创建人ID（教师/管理员）
     */
    private Long createUserId;

    /**
     * 是否允许查看答案：0-不允许, 1-允许
     */
    private Integer allowViewAnswer;

    /**
     * 是否允许补考：0-不允许, 1-允许
     */
    private Integer allowRetake;

    /**
     * 创建时间
     */
    private LocalDateTime createTime;

    /**
     * 更新时间
     */
    private LocalDateTime updateTime;
}


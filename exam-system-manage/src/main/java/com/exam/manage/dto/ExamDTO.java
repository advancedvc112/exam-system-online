package com.exam.manage.dto;

import lombok.Data;
import java.time.LocalDateTime;

/**
 * 考试安排DTO
 */
@Data
public class ExamDTO {
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
     * 试卷名称（查询时填充）
     */
    private String paperName;

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
     * 考试状态
     */
    private String status;

    /**
     * 创建人ID
     */
    private Long createUserId;

    /**
     * 创建人姓名（查询时填充）
     */
    private String createUserName;

    /**
     * 是否允许查看答案
     */
    private Integer allowViewAnswer;

    /**
     * 是否允许补考
     */
    private Integer allowRetake;
}


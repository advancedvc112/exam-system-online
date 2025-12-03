package com.exam.excute.dal.dataobject;

import lombok.Data;
import java.time.LocalDateTime;

/**
 * 考试记录实体类
 */
@Data
public class ExamRecordDO {
    /**
     * 记录ID
     */
    private Long id;

    /**
     * 考试ID
     */
    private Long examId;

    /**
     * 学生ID
     */
    private Long studentId;

    /**
     * 试卷ID
     */
    private Long paperId;

    /**
     * 开始答题时间
     */
    private LocalDateTime startTime;

    /**
     * 提交时间
     */
    private LocalDateTime submitTime;

    /**
     * 总分数
     */
    private Integer totalScore;

    /**
     * 得分
     */
    private Integer score;

    /**
     * 状态：in_progress-答题中, submitted-已提交, timeout-超时, cancelled-取消
     */
    private String status;

    /**
     * 切屏次数
     */
    private Integer switchCount;

    /**
     * 是否作弊：0-否, 1-是
     */
    private Integer isCheating;

    /**
     * 作弊原因
     */
    private String cheatingReason;

    /**
     * 创建时间
     */
    private LocalDateTime createTime;

    /**
     * 更新时间
     */
    private LocalDateTime updateTime;
}


package com.exam.excute.dal.dataobject;

import lombok.Data;
import java.time.LocalDateTime;

/**
 * 答题记录实体类
 */
@Data
public class AnswerRecordDO {
    /**
     * 记录ID
     */
    private Long id;

    /**
     * 考试记录ID
     */
    private Long examRecordId;

    /**
     * 题目ID
     */
    private Long questionId;

    /**
     * 学生答案
     */
    private String studentAnswer;

    /**
     * 是否正确：0-错误, 1-正确, 2-部分正确（多选题）
     */
    private Integer isCorrect;

    /**
     * 得分
     */
    private Integer score;

    /**
     * 答题时间（毫秒）
     */
    private Long answerTime;

    /**
     * 创建时间
     */
    private LocalDateTime createTime;

    /**
     * 更新时间
     */
    private LocalDateTime updateTime;
}


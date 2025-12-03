package com.exam.excute.dto;

import lombok.Data;

/**
 * 答题DTO
 */
@Data
public class AnswerDTO {
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
}


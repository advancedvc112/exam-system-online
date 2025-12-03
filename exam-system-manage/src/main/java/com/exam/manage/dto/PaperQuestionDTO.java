package com.exam.manage.dto;

import lombok.Data;

/**
 * 试卷题目DTO
 */
@Data
public class PaperQuestionDTO {
    /**
     * 题目ID
     */
    private Long questionId;

    /**
     * 题目序号
     */
    private Integer orderNum;

    /**
     * 分值
     */
    private Integer score;
}


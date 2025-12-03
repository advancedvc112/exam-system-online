package com.exam.manage.dto;

import lombok.Data;

/**
 * 题目DTO
 */
@Data
public class QuestionDTO {
    /**
     * 题目ID
     */
    private Long id;

    /**
     * 题目内容
     */
    private String content;

    /**
     * 题目类型
     */
    private String type;

    /**
     * 题目难度
     */
    private String difficulty;

    /**
     * 选项（JSON格式）
     */
    private String options;

    /**
     * 正确答案
     */
    private String answer;

    /**
     * 分值
     */
    private Integer score;

    /**
     * 题目分类
     */
    private String category;

    /**
     * 创建人ID
     */
    private Long createUserId;

    /**
     * 状态
     */
    private Integer status;
}


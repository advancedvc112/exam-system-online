package com.exam.manage.dto;

import lombok.Data;

/**
 * 随机组卷规则DTO
 */
@Data
public class RandomRuleDTO {
    /**
     * 题目类型
     */
    private String type;

    /**
     * 题目难度
     */
    private String difficulty;

    /**
     * 题目分类（可选）
     */
    private String category;

    /**
     * 题目数量
     */
    private Integer count;

    /**
     * 每题分值
     */
    private Integer score;
}


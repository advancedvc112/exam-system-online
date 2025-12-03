package com.exam.manage.dto;

import lombok.Data;

/**
 * 题目查询DTO
 */
@Data
public class QuestionQueryDTO {
    /**
     * 题目类型
     */
    private String type;

    /**
     * 题目难度
     */
    private String difficulty;

    /**
     * 题目分类
     */
    private String category;

    /**
     * 页码（从1开始）
     */
    private Integer pageNum = 1;

    /**
     * 每页数量
     */
    private Integer pageSize = 10;
}


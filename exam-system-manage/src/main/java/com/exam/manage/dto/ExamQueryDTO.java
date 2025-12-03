package com.exam.manage.dto;

import lombok.Data;

/**
 * 考试安排查询DTO
 */
@Data
public class ExamQueryDTO {
    /**
     * 考试状态
     */
    private String status;

    /**
     * 创建人ID
     */
    private Long createUserId;

    /**
     * 页码（从1开始）
     */
    private Integer pageNum = 1;

    /**
     * 每页数量
     */
    private Integer pageSize = 10;
}


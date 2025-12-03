package com.exam.manage.dto;

import lombok.Data;
import java.util.List;

/**
 * 题目分页DTO
 */
@Data
public class QuestionPageDTO {
    /**
     * 题目列表
     */
    private List<QuestionDTO> list;

    /**
     * 总记录数
     */
    private Long total;

    /**
     * 当前页码
     */
    private Integer pageNum;

    /**
     * 每页数量
     */
    private Integer pageSize;

    /**
     * 总页数
     */
    private Integer totalPages;
}


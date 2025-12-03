package com.exam.manage.dto;

import lombok.Data;
import java.util.List;

/**
 * 考试安排分页DTO
 */
@Data
public class ExamPageDTO {
    /**
     * 考试列表
     */
    private List<ExamDTO> list;

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


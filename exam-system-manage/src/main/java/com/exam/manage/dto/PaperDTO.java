package com.exam.manage.dto;

import lombok.Data;
import java.util.List;

/**
 * 试卷DTO
 */
@Data
public class PaperDTO {
    /**
     * 试卷ID
     */
    private Long id;

    /**
     * 试卷名称
     */
    private String name;

    /**
     * 试卷描述
     */
    private String description;

    /**
     * 试卷类型
     */
    private String type;

    /**
     * 总分数
     */
    private Integer totalScore;

    /**
     * 考试时长（分钟）
     */
    private Integer duration;

    /**
     * 创建人ID
     */
    private Long createUserId;

    /**
     * 状态
     */
    private Integer status;

    /**
     * 题目列表（用于固定组卷）
     */
    private List<PaperQuestionDTO> questions;
}


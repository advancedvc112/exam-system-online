package com.exam.manage.dto;

import lombok.Data;
import java.util.List;

/**
 * 随机组卷DTO
 */
@Data
public class RandomPaperDTO {
    /**
     * 试卷名称
     */
    private String name;

    /**
     * 试卷描述
     */
    private String description;

    /**
     * 考试时长（分钟）
     */
    private Integer duration;

    /**
     * 创建人ID
     */
    private Long createUserId;

    /**
     * 随机组卷规则
     */
    private List<RandomRuleDTO> rules;
}


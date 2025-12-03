package com.exam.manage.dal.dataobject;

import lombok.Data;

/**
 * 试卷题目关联实体类
 */
@Data
public class PaperQuestionDO {
    /**
     * 关联ID
     */
    private Long id;

    /**
     * 试卷ID
     */
    private Long paperId;

    /**
     * 题目ID
     */
    private Long questionId;

    /**
     * 题目序号（在试卷中的顺序）
     */
    private Integer orderNum;

    /**
     * 该题在试卷中的分值（可以覆盖题目的默认分值）
     */
    private Integer score;
}


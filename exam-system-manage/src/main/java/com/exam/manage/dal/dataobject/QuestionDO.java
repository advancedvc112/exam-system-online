package com.exam.manage.dal.dataobject;

import lombok.Data;
import java.time.LocalDateTime;

/**
 * 题目实体类
 */
@Data
public class QuestionDO {
    /**
     * 题目ID
     */
    private Long id;

    /**
     * 题目内容
     */
    private String content;

    /**
     * 题目类型：single_choice-单选, multiple_choice-多选, true_false-判断, fill_blank-填空, short_answer-简答
     */
    private String type;

    /**
     * 题目难度：easy-简单, medium-中等, hard-困难
     */
    private String difficulty;

    /**
     * 选项（JSON格式，用于单选和多选）
     * 例如：{"A":"选项A内容","B":"选项B内容","C":"选项C内容","D":"选项D内容"}
     */
    private String options;

    /**
     * 正确答案
     * 单选：A/B/C/D
     * 多选：A,B,C（逗号分隔）
     * 判断：true/false
     * 填空：答案内容
     * 简答：参考答案
     */
    private String answer;

    /**
     * 分值
     */
    private Integer score;

    /**
     * 题目分类/知识点
     */
    private String category;

    /**
     * 创建人ID
     */
    private Long createUserId;

    /**
     * 状态：0-禁用，1-启用
     */
    private Integer status;

    /**
     * 创建时间
     */
    private LocalDateTime createTime;

    /**
     * 更新时间
     */
    private LocalDateTime updateTime;
}


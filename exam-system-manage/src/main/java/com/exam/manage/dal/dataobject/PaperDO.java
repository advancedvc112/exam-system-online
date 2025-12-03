package com.exam.manage.dal.dataobject;

import lombok.Data;
import java.time.LocalDateTime;

/**
 * 试卷实体类
 */
@Data
public class PaperDO {
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
     * 试卷类型：random-随机组卷, fixed-固定组卷
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


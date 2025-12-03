package com.exam.excute.dto;

import lombok.Data;

/**
 * 考试记录概要信息（用于前端获取试卷ID等）
 */
@Data
public class ExamRecordInfoDTO {

    /**
     * 考试记录ID
     */
    private Long examRecordId;

    /**
     * 考试ID
     */
    private Long examId;

    /**
     * 试卷ID
     */
    private Long paperId;
}



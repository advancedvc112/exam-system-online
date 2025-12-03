package com.exam.excute.controller.admin.controller;

import com.exam.excute.dto.AnswerDTO;
import com.exam.excute.service.ExamSyncService;
import com.exam.userService.dto.Result;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

/**
 * 考试执行控制器
 */
@RestController
@RequestMapping("/exam-online/execute")
public class ExamExecuteController {

    @Autowired
    private ExamSyncService examSyncService;

    /**
     * 保存答案（REST接口，作为WebSocket的补充）
     */
    @PostMapping("/answer")
    public Result<Object> saveAnswer(@RequestBody AnswerDTO answerDTO) {
        examSyncService.saveAnswerRealtime(
            answerDTO.getExamRecordId(),
            answerDTO.getQuestionId(),
            answerDTO.getStudentAnswer()
        );
        return Result.success("答案保存成功");
    }

    /**
     * 获取答题进度
     */
    @GetMapping("/progress/{examRecordId}")
    public Result<Long> getProgress(@PathVariable Long examRecordId) {
        Long progress = examSyncService.getProgress(examRecordId);
        return Result.success(progress);
    }
}


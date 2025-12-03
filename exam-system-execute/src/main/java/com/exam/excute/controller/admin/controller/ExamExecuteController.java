package com.exam.excute.controller.admin.controller;

import com.exam.excute.config.AuthUtil;
import com.exam.excute.dto.AnswerDTO;
import com.exam.excute.dto.ExamRecordInfoDTO;
import com.exam.excute.dal.dataobject.ExamRecordDO;
import com.exam.excute.dal.mysqlmapper.ExamRecordMapper;
import com.exam.excute.service.ExamSyncService;
import com.exam.manage.dal.dataobject.ExamDO;
import com.exam.manage.dal.mysqlmapper.ExamMapper;
import com.exam.userService.dto.Result;
import jakarta.servlet.http.HttpServletRequest;
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

    @Autowired
    private AuthUtil authUtil;

    @Autowired
    private ExamMapper examMapper;

    @Autowired
    private ExamRecordMapper examRecordMapper;

    /**
     * 学生进入考试：创建或获取考试记录ID
     */
    @PostMapping("/start/{examId}")
    public Result<Long> startExam(@PathVariable Long examId, HttpServletRequest request) {
        // 必须是学生
        Long studentId = authUtil.checkStudent(request);

        // 查询考试信息，确认考试存在且进行中
        ExamDO exam = examMapper.selectById(examId);
        if (exam == null) {
            throw new RuntimeException("考试不存在");
        }
        if (!"in_progress".equals(exam.getStatus())) {
            throw new RuntimeException("考试未开始或已结束，无法进入");
        }

        Long examRecordId = examSyncService.startExam(examId, studentId, exam.getPaperId());
        return Result.success(examRecordId);
    }

    /**
     * 根据考试记录ID获取考试/试卷等基础信息
     */
    @GetMapping("/record/{examRecordId}")
    public Result<ExamRecordInfoDTO> getExamRecordInfo(@PathVariable Long examRecordId,
                                                       HttpServletRequest request) {
        Long studentId = authUtil.checkStudent(request);

        ExamRecordDO record = examRecordMapper.selectById(examRecordId);
        if (record == null || !studentId.equals(record.getStudentId())) {
            throw new RuntimeException("考试记录不存在或无权访问");
        }

        ExamRecordInfoDTO dto = new ExamRecordInfoDTO();
        dto.setExamRecordId(record.getId());
        dto.setExamId(record.getExamId());
        dto.setPaperId(record.getPaperId());
        return Result.success(dto);
    }

    /**
     * 保存答案（REST接口，作为WebSocket的补充）
     */
    @PostMapping("/answer")
    public Result<Object> saveAnswer(@RequestBody AnswerDTO answerDTO, HttpServletRequest request) {
        // 只有学生可以提交答案
        authUtil.checkStudent(request);
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
    public Result<Long> getProgress(@PathVariable Long examRecordId, HttpServletRequest request) {
        // 只有学生可以查看自己的答题进度
        authUtil.checkStudent(request);
        Long progress = examSyncService.getProgress(examRecordId);
        return Result.success(progress);
    }
}


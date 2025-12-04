package com.exam.excute.controller.admin.controller;

import com.exam.excute.config.AuthUtil;
import com.exam.excute.dto.AnswerDTO;
import com.exam.excute.dto.ExamRecordInfoDTO;
import com.exam.excute.dal.dataobject.ExamRecordDO;
import com.exam.excute.dal.mysqlmapper.ExamRecordMapper;
import com.exam.excute.service.ExamSyncService;
import com.exam.excute.service.ExamTokenService;
import com.exam.excute.util.DistributedLockUtil;
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

    @Autowired
    private ExamTokenService examTokenService;

    @Autowired
    private DistributedLockUtil distributedLockUtil;

    /**
     * 获取考试令牌（学生进入考试前需要先获取令牌）
     * 只有进行中的考试才能获取令牌
     * 使用分布式锁防止重复获取
     */
    @GetMapping("/token/{examId}")
    public Result<String> getExamToken(@PathVariable Long examId, HttpServletRequest request) {
        // 验证学生身份
        Long studentId = authUtil.checkStudent(request);
        
        // 使用分布式锁，防止同一学生重复获取令牌
        String lockKey = "lock:exam:token:" + examId + ":" + studentId;
        
        return distributedLockUtil.executeWithLock(lockKey, 10, () -> {
            // 查询考试信息
            ExamDO exam = examMapper.selectById(examId);
            if (exam == null) {
                throw new RuntimeException("考试不存在");
            }
            
            // 只有进行中的考试才能获取令牌
            if (!"in_progress".equals(exam.getStatus())) {
                throw new RuntimeException("考试未开始或已结束，无法获取令牌");
            }
            
            // 从Redis获取令牌
            String token = examTokenService.getToken(examId);
            
            // 如果令牌不存在，自动生成一个（兜底处理：可能是定时任务开启了考试但未生成令牌，或令牌过期）
            if (token == null) {
                token = examTokenService.issueToken(examId, exam.getEndTime());
            }
            
            return Result.success("获取令牌成功", token);
        });
    }

    /**
     * 学生进入考试：创建或获取考试记录ID
     * 需要提供考试令牌（从请求头 X-Exam-Token 获取）
     */
    @PostMapping("/start/{examId}")
    public Result<Long> startExam(@PathVariable Long examId, 
                                  @RequestHeader(value = "X-Exam-Token", required = false) String examToken,
                                  HttpServletRequest request) {
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

        // 验证考试令牌
        if (examToken == null || examToken.trim().isEmpty()) {
            throw new RuntimeException("考试令牌不能为空");
        }
        if (!examTokenService.validateToken(examId, examToken)) {
            throw new RuntimeException("考试令牌无效或已过期，请重新获取");
        }

        // 使用分布式锁，防止同一学生重复进入考试
        String lockKey = "lock:exam:start:" + examId + ":" + studentId;
        
        Long examRecordId = distributedLockUtil.executeWithLock(lockKey, 10, () -> {
            return examSyncService.startExam(examId, studentId, exam.getPaperId());
        });
        
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
     * 需要提供考试令牌（从请求头 X-Exam-Token 获取）
     */
    @PostMapping("/answer")
    public Result<Object> saveAnswer(@RequestBody AnswerDTO answerDTO,
                                     @RequestHeader(value = "X-Exam-Token", required = false) String examToken,
                                     HttpServletRequest request) {
        // 只有学生可以提交答案
        authUtil.checkStudent(request);
        
        // 验证令牌
        if (examToken == null || examToken.trim().isEmpty()) {
            throw new RuntimeException("考试令牌不能为空");
        }
        
        examSyncService.saveAnswerRealtime(
            answerDTO.getExamRecordId(),
            answerDTO.getQuestionId(),
            answerDTO.getStudentAnswer(),
            examToken
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

    /**
     * 考生提前结束考试（直接提交答案到数据库，清除该考生的考试令牌）
     * 需要提供考试令牌（从请求头 X-Exam-Token 获取）
     */
    @PostMapping("/submit/{examRecordId}")
    public Result<Object> submitExam(@PathVariable Long examRecordId,
                                     @RequestHeader(value = "X-Exam-Token", required = false) String examToken,
                                     HttpServletRequest request) {
        // 必须是学生
        Long studentId = authUtil.checkStudent(request);
        
        // 验证考试记录
        ExamRecordDO record = examRecordMapper.selectById(examRecordId);
        if (record == null || !studentId.equals(record.getStudentId())) {
            throw new RuntimeException("考试记录不存在或无权访问");
        }
        
        if (!"in_progress".equals(record.getStatus())) {
            throw new RuntimeException("考试已提交或已结束，无法重复提交");
        }
        
        // 验证令牌
        if (examToken == null || examToken.trim().isEmpty()) {
            throw new RuntimeException("考试令牌不能为空");
        }
        if (!examTokenService.validateToken(record.getExamId(), examToken)) {
            throw new RuntimeException("考试令牌无效或已过期");
        }
        
        // 提前结束考试：直接提交答案到数据库
        examSyncService.submitExamEarly(examRecordId, examToken, record.getExamId());
        
        return Result.success("考试提交成功");
    }
}


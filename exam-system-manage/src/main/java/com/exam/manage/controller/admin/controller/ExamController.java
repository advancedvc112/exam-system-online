package com.exam.manage.controller.admin.controller;

import com.exam.manage.config.AuthUtil;
import com.exam.manage.dto.ExamDTO;
import com.exam.manage.dto.ExamPageDTO;
import com.exam.manage.dto.ExamQueryDTO;
import com.exam.manage.service.ExamService;
import com.exam.userService.dto.Result;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

/**
 * 考试安排控制器
 */
@RestController
@RequestMapping("/exam-online/manage/exam")
public class ExamController {

    @Autowired
    private ExamService examService;

    @Autowired
    private AuthUtil authUtil;

    /**
     * 创建考试安排
     */
    @PostMapping("/create")
    public Result<ExamDTO> createExam(@RequestBody ExamDTO examDTO, HttpServletRequest request) {
        // 只有管理员或教师可以创建考试
        authUtil.checkAdminOrTeacher(request);
        ExamDTO result = examService.createExam(examDTO);
        return Result.success("创建成功", result);
    }

    /**
     * 根据ID查询考试安排
     */
    @GetMapping("/{id}")
    public Result<ExamDTO> getExamById(@PathVariable Long id) {
        ExamDTO examDTO = examService.getExamById(id);
        return Result.success(examDTO);
    }

    /**
     * 更新考试安排
     */
    @PutMapping("/update")
    public Result<ExamDTO> updateExam(@RequestBody ExamDTO examDTO, HttpServletRequest request) {
        // 只有管理员或教师可以更新考试
        authUtil.checkAdminOrTeacher(request);
        ExamDTO result = examService.updateExam(examDTO);
        return Result.success("更新成功", result);
    }

    /**
     * 删除考试安排（取消考试）
     */
    @DeleteMapping("/{id}")
    public Result<Object> deleteExam(@PathVariable Long id, HttpServletRequest request) {
        // 只有管理员或教师可以删除/取消考试
        authUtil.checkAdminOrTeacher(request);
        examService.deleteExam(id);
        return Result.success("删除成功");
    }

    /**
     * 分页查询考试安排列表
     */
    @PostMapping("/list")
    public Result<ExamPageDTO> getExamList(@RequestBody ExamQueryDTO queryDTO) {
        ExamPageDTO pageDTO = examService.getExamList(queryDTO);
        return Result.success(pageDTO);
    }

    /**
     * 更新单个考试状态（开启/结束考试）
     * 如果开启了考试，会返回考试令牌
     */
    @PutMapping("/{id}/status")
    public Result<Object> updateExamStatus(@PathVariable Long id, HttpServletRequest request) {
        // 只有管理员或教师可以手动更新考试状态（视为“开启/结束考试”等操作）
        authUtil.checkAdminOrTeacher(request);
        String token = examService.updateExamStatus(id);
        
        if (token != null) {
            // 开启了考试，返回令牌
            return Result.success("考试已开启，令牌已生成", token);
        } else {
            // 结束了考试或其他操作
            return Result.success("状态更新成功");
        }
    }

    /**
     * 批量更新考试状态（用于定时任务）
     */
    @PutMapping("/batch-update-status")
    public Result<Object> batchUpdateExamStatus() {
        examService.batchUpdateExamStatus();
        return Result.success("批量状态更新成功");
    }
}


package com.exam.manage.controller.admin.controller;

import com.exam.manage.dto.ExamDTO;
import com.exam.manage.dto.ExamPageDTO;
import com.exam.manage.dto.ExamQueryDTO;
import com.exam.manage.service.ExamService;
import com.exam.userService.dto.Result;
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

    /**
     * 创建考试安排
     */
    @PostMapping("/create")
    public Result<ExamDTO> createExam(@RequestBody ExamDTO examDTO) {
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
    public Result<ExamDTO> updateExam(@RequestBody ExamDTO examDTO) {
        ExamDTO result = examService.updateExam(examDTO);
        return Result.success("更新成功", result);
    }

    /**
     * 删除考试安排（取消考试）
     */
    @DeleteMapping("/{id}")
    public Result<Object> deleteExam(@PathVariable Long id) {
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
     * 更新单个考试状态
     */
    @PutMapping("/{id}/status")
    public Result<Object> updateExamStatus(@PathVariable Long id) {
        examService.updateExamStatus(id);
        return Result.success("状态更新成功");
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


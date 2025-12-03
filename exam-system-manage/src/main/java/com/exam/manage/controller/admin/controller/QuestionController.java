package com.exam.manage.controller.admin.controller;

import com.exam.manage.dto.QuestionDTO;
import com.exam.manage.dto.QuestionPageDTO;
import com.exam.manage.dto.QuestionQueryDTO;
import com.exam.manage.service.QuestionService;
import com.exam.userService.dto.Result;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

/**
 * 题目管理控制器
 */
@RestController
@RequestMapping("/exam-online/manage/question")
public class QuestionController {

    @Autowired
    private QuestionService questionService;

    /**
     * 添加题目
     */
    @PostMapping("/add")
    public Result<QuestionDTO> addQuestion(@RequestBody QuestionDTO questionDTO) {
        QuestionDTO result = questionService.addQuestion(questionDTO);
        return Result.success("添加成功", result);
    }

    /**
     * 根据ID查询题目
     */
    @GetMapping("/{id}")
    public Result<QuestionDTO> getQuestionById(@PathVariable Long id) {
        QuestionDTO questionDTO = questionService.getQuestionById(id);
        return Result.success(questionDTO);
    }

    /**
     * 更新题目
     */
    @PutMapping("/update")
    public Result<QuestionDTO> updateQuestion(@RequestBody QuestionDTO questionDTO) {
        QuestionDTO result = questionService.updateQuestion(questionDTO);
        return Result.success("更新成功", result);
    }

    /**
     * 删除题目
     */
    @DeleteMapping("/{id}")
    public Result<Object> deleteQuestion(@PathVariable Long id) {
        questionService.deleteQuestion(id);
        return Result.success("删除成功");
    }

    /**
     * 分页查询题目列表
     */
    @PostMapping("/list")
    public Result<QuestionPageDTO> getQuestionList(@RequestBody QuestionQueryDTO queryDTO) {
        QuestionPageDTO pageDTO = questionService.getQuestionList(queryDTO);
        return Result.success(pageDTO);
    }
}


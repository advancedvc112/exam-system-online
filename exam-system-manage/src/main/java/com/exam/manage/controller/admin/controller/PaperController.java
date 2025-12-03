package com.exam.manage.controller.admin.controller;

import com.exam.manage.dto.PaperDTO;
import com.exam.manage.dto.RandomPaperDTO;
import com.exam.manage.service.PaperService;
import com.exam.userService.dto.Result;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 试卷管理控制器
 */
@RestController
@RequestMapping("/exam-online/manage/paper")
public class PaperController {

    @Autowired
    private PaperService paperService;

    /**
     * 随机组卷
     */
    @PostMapping("/random")
    public Result<PaperDTO> createRandomPaper(@RequestBody RandomPaperDTO randomPaperDTO) {
        PaperDTO paperDTO = paperService.createRandomPaper(randomPaperDTO);
        return Result.success("随机组卷成功", paperDTO);
    }

    /**
     * 固定组卷
     */
    @PostMapping("/fixed")
    public Result<PaperDTO> createFixedPaper(@RequestBody PaperDTO paperDTO) {
        PaperDTO result = paperService.createFixedPaper(paperDTO);
        return Result.success("固定组卷成功", result);
    }

    /**
     * 根据ID查询试卷
     */
    @GetMapping("/{id}")
    public Result<PaperDTO> getPaperById(@PathVariable Long id) {
        PaperDTO paperDTO = paperService.getPaperById(id);
        return Result.success(paperDTO);
    }

    /**
     * 更新试卷（仅限固定组卷）
     */
    @PutMapping("/update")
    public Result<PaperDTO> updatePaper(@RequestBody PaperDTO paperDTO) {
        PaperDTO result = paperService.updatePaper(paperDTO);
        return Result.success("更新成功", result);
    }

    /**
     * 删除试卷
     */
    @DeleteMapping("/{id}")
    public Result<Object> deletePaper(@PathVariable Long id) {
        paperService.deletePaper(id);
        return Result.success("删除成功");
    }

    /**
     * 分页查询试卷列表
     */
    @GetMapping("/list")
    public Result<List<PaperDTO>> getPaperList(
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize) {
        List<PaperDTO> paperList = paperService.getPaperList(pageNum, pageSize);
        return Result.success(paperList);
    }
}


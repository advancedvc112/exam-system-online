package com.exam.manage.service;

import com.exam.manage.dal.dataobject.PaperDO;
import com.exam.manage.dal.dataobject.PaperQuestionDO;
import com.exam.manage.dal.dataobject.QuestionDO;
import com.exam.manage.dal.mysqlmapper.PaperMapper;
import com.exam.manage.dal.mysqlmapper.PaperQuestionMapper;
import com.exam.manage.dal.mysqlmapper.QuestionMapper;
import com.exam.manage.dto.*;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 试卷服务类
 */
@Service
public class PaperService {

    @Autowired
    private PaperMapper paperMapper;

    @Autowired
    private PaperQuestionMapper paperQuestionMapper;

    @Autowired
    private QuestionMapper questionMapper;

    /**
     * 随机组卷
     */
    @Transactional
    public PaperDTO createRandomPaper(RandomPaperDTO randomPaperDTO) {
        // 1. 创建试卷基本信息
        PaperDO paperDO = new PaperDO();
        paperDO.setName(randomPaperDTO.getName());
        paperDO.setDescription(randomPaperDTO.getDescription());
        paperDO.setType("random");
        paperDO.setDuration(randomPaperDTO.getDuration());
        paperDO.setCreateUserId(randomPaperDTO.getCreateUserId());
        paperDO.setStatus(1);

        // 2. 根据规则随机选择题目
        List<PaperQuestionDO> paperQuestionList = new ArrayList<>();
        int totalScore = 0;
        int orderNum = 1;

        for (RandomRuleDTO rule : randomPaperDTO.getRules()) {
            // 查询符合条件的题目数量
            Long availableCount = questionMapper.count(
                rule.getType(),
                rule.getDifficulty(),
                rule.getCategory()
            );

            if (availableCount < rule.getCount()) {
                throw new RuntimeException(
                    String.format("题目数量不足：需要%d道，但只有%d道（类型：%s，难度：%s）",
                        rule.getCount(), availableCount, rule.getType(), rule.getDifficulty())
                );
            }

            // 随机选择题目
            List<QuestionDO> questions = questionMapper.selectRandom(
                rule.getType(),
                rule.getDifficulty(),
                rule.getCategory(),
                rule.getCount()
            );

            // 添加到试卷中
            for (QuestionDO question : questions) {
                PaperQuestionDO paperQuestion = new PaperQuestionDO();
                paperQuestion.setPaperId(null); // 先设为null，插入试卷后设置
                paperQuestion.setQuestionId(question.getId());
                paperQuestion.setOrderNum(orderNum++);
                paperQuestion.setScore(rule.getScore() != null ? rule.getScore() : question.getScore());
                paperQuestionList.add(paperQuestion);
                totalScore += paperQuestion.getScore();
            }
        }

        // 3. 设置总分数并插入试卷
        paperDO.setTotalScore(totalScore);
        paperMapper.insert(paperDO);

        // 4. 设置试卷ID并批量插入题目关联
        for (PaperQuestionDO paperQuestion : paperQuestionList) {
            paperQuestion.setPaperId(paperDO.getId());
        }
        paperQuestionMapper.batchInsert(paperQuestionList);

        // 5. 构建返回结果
        PaperDTO paperDTO = new PaperDTO();
        BeanUtils.copyProperties(paperDO, paperDTO);
        paperDTO.setQuestions(paperQuestionList.stream()
            .map(pq -> {
                PaperQuestionDTO dto = new PaperQuestionDTO();
                dto.setQuestionId(pq.getQuestionId());
                dto.setOrderNum(pq.getOrderNum());
                dto.setScore(pq.getScore());
                return dto;
            })
            .collect(Collectors.toList()));

        return paperDTO;
    }

    /**
     * 固定组卷
     */
    @Transactional
    public PaperDTO createFixedPaper(PaperDTO paperDTO) {
        // 1. 创建试卷基本信息
        PaperDO paperDO = new PaperDO();
        BeanUtils.copyProperties(paperDTO, paperDO);
        paperDO.setType("fixed");
        paperDO.setStatus(1);

        // 2. 计算总分数
        int totalScore = paperDTO.getQuestions().stream()
            .mapToInt(PaperQuestionDTO::getScore)
            .sum();
        paperDO.setTotalScore(totalScore);

        // 3. 验证题目是否存在
        for (PaperQuestionDTO questionDTO : paperDTO.getQuestions()) {
            QuestionDO question = questionMapper.selectById(questionDTO.getQuestionId());
            if (question == null || question.getStatus() == 0) {
                throw new RuntimeException("题目不存在或已禁用，题目ID：" + questionDTO.getQuestionId());
            }
        }

        // 4. 插入试卷
        paperMapper.insert(paperDO);

        // 5. 批量插入题目关联
        List<PaperQuestionDO> paperQuestionList = new ArrayList<>();
        for (int i = 0; i < paperDTO.getQuestions().size(); i++) {
            PaperQuestionDTO questionDTO = paperDTO.getQuestions().get(i);
            PaperQuestionDO paperQuestion = new PaperQuestionDO();
            paperQuestion.setPaperId(paperDO.getId());
            paperQuestion.setQuestionId(questionDTO.getQuestionId());
            paperQuestion.setOrderNum(questionDTO.getOrderNum() != null ? questionDTO.getOrderNum() : (i + 1));
            paperQuestion.setScore(questionDTO.getScore());
            paperQuestionList.add(paperQuestion);
        }
        paperQuestionMapper.batchInsert(paperQuestionList);

        // 6. 构建返回结果
        PaperDTO result = new PaperDTO();
        BeanUtils.copyProperties(paperDO, result);
        result.setQuestions(paperDTO.getQuestions());
        return result;
    }

    /**
     * 根据ID查询试卷（包含题目列表）
     */
    public PaperDTO getPaperById(Long id) {
        PaperDO paperDO = paperMapper.selectById(id);
        if (paperDO == null) {
            throw new RuntimeException("试卷不存在");
        }

        // 查询试卷的题目列表
        List<PaperQuestionDO> paperQuestionList = paperQuestionMapper.selectByPaperId(id);

        PaperDTO paperDTO = new PaperDTO();
        BeanUtils.copyProperties(paperDO, paperDTO);
        paperDTO.setQuestions(paperQuestionList.stream()
            .map(pq -> {
                PaperQuestionDTO dto = new PaperQuestionDTO();
                dto.setQuestionId(pq.getQuestionId());
                dto.setOrderNum(pq.getOrderNum());
                dto.setScore(pq.getScore());
                return dto;
            })
            .collect(Collectors.toList()));

        return paperDTO;
    }

    /**
     * 更新试卷（仅限固定组卷）
     */
    @Transactional
    public PaperDTO updatePaper(PaperDTO paperDTO) {
        PaperDO existingPaper = paperMapper.selectById(paperDTO.getId());
        if (existingPaper == null) {
            throw new RuntimeException("试卷不存在");
        }

        if (!"fixed".equals(existingPaper.getType())) {
            throw new RuntimeException("只能修改固定组卷的试卷");
        }

        // 更新试卷基本信息
        PaperDO paperDO = new PaperDO();
        BeanUtils.copyProperties(paperDTO, paperDO);

        // 重新计算总分数
        int totalScore = paperDTO.getQuestions().stream()
            .mapToInt(PaperQuestionDTO::getScore)
            .sum();
        paperDO.setTotalScore(totalScore);

        // 验证题目
        for (PaperQuestionDTO questionDTO : paperDTO.getQuestions()) {
            QuestionDO question = questionMapper.selectById(questionDTO.getQuestionId());
            if (question == null || question.getStatus() == 0) {
                throw new RuntimeException("题目不存在或已禁用，题目ID：" + questionDTO.getQuestionId());
            }
        }

        // 更新试卷
        paperMapper.update(paperDO);

        // 删除旧的题目关联
        paperQuestionMapper.deleteByPaperId(paperDTO.getId());

        // 插入新的题目关联
        List<PaperQuestionDO> paperQuestionList = new ArrayList<>();
        for (int i = 0; i < paperDTO.getQuestions().size(); i++) {
            PaperQuestionDTO questionDTO = paperDTO.getQuestions().get(i);
            PaperQuestionDO paperQuestion = new PaperQuestionDO();
            paperQuestion.setPaperId(paperDTO.getId());
            paperQuestion.setQuestionId(questionDTO.getQuestionId());
            paperQuestion.setOrderNum(questionDTO.getOrderNum() != null ? questionDTO.getOrderNum() : (i + 1));
            paperQuestion.setScore(questionDTO.getScore());
            paperQuestionList.add(paperQuestion);
        }
        paperQuestionMapper.batchInsert(paperQuestionList);

        return getPaperById(paperDTO.getId());
    }

    /**
     * 删除试卷
     */
    @Transactional
    public void deletePaper(Long id) {
        PaperDO paperDO = paperMapper.selectById(id);
        if (paperDO == null) {
            throw new RuntimeException("试卷不存在");
        }
        paperMapper.deleteById(id);
        // 删除题目关联
        paperQuestionMapper.deleteByPaperId(id);
    }

    /**
     * 分页查询试卷列表
     */
    public List<PaperDTO> getPaperList(Integer pageNum, Integer pageSize) {
        Integer offset = (pageNum - 1) * pageSize;
        List<PaperDO> paperDOList = paperMapper.selectList(offset, pageSize);

        return paperDOList.stream()
            .map(paperDO -> {
                PaperDTO paperDTO = new PaperDTO();
                BeanUtils.copyProperties(paperDO, paperDTO);
                return paperDTO;
            })
            .collect(Collectors.toList());
    }
}


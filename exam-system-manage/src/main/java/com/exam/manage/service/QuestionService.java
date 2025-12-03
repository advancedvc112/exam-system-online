package com.exam.manage.service;

import com.exam.manage.dal.dataobject.QuestionDO;
import com.exam.manage.dal.mysqlmapper.QuestionMapper;
import com.exam.manage.dto.QuestionDTO;
import com.exam.manage.dto.QuestionPageDTO;
import com.exam.manage.dto.QuestionQueryDTO;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 题目服务类
 */
@Service
public class QuestionService {

    @Autowired
    private QuestionMapper questionMapper;

    /**
     * 添加题目
     */
    public QuestionDTO addQuestion(QuestionDTO questionDTO) {
        QuestionDO questionDO = new QuestionDO();
        BeanUtils.copyProperties(questionDTO, questionDO);
        questionDO.setStatus(1); // 默认启用
        questionMapper.insert(questionDO);
        
        QuestionDTO result = new QuestionDTO();
        BeanUtils.copyProperties(questionDO, result);
        return result;
    }

    /**
     * 根据ID查询题目
     */
    public QuestionDTO getQuestionById(Long id) {
        QuestionDO questionDO = questionMapper.selectById(id);
        if (questionDO == null) {
            throw new RuntimeException("题目不存在");
        }
        QuestionDTO questionDTO = new QuestionDTO();
        BeanUtils.copyProperties(questionDO, questionDTO);
        return questionDTO;
    }

    /**
     * 更新题目
     */
    public QuestionDTO updateQuestion(QuestionDTO questionDTO) {
        QuestionDO existingQuestion = questionMapper.selectById(questionDTO.getId());
        if (existingQuestion == null) {
            throw new RuntimeException("题目不存在");
        }

        QuestionDO questionDO = new QuestionDO();
        BeanUtils.copyProperties(questionDTO, questionDO);
        questionMapper.update(questionDO);
        
        return getQuestionById(questionDTO.getId());
    }

    /**
     * 删除题目
     */
    public void deleteQuestion(Long id) {
        QuestionDO questionDO = questionMapper.selectById(id);
        if (questionDO == null) {
            throw new RuntimeException("题目不存在");
        }
        questionMapper.deleteById(id);
    }

    /**
     * 分页查询题目列表
     */
    public QuestionPageDTO getQuestionList(QuestionQueryDTO queryDTO) {
        // 计算偏移量
        Integer offset = (queryDTO.getPageNum() - 1) * queryDTO.getPageSize();
        
        // 查询列表
        List<QuestionDO> questionDOList = questionMapper.selectList(
            queryDTO.getType(),
            queryDTO.getDifficulty(),
            queryDTO.getCategory(),
            offset,
            queryDTO.getPageSize()
        );

        // 查询总数
        Long total = questionMapper.count(
            queryDTO.getType(),
            queryDTO.getDifficulty(),
            queryDTO.getCategory()
        );

        // 转换为DTO
        List<QuestionDTO> questionDTOList = questionDOList.stream()
            .map(questionDO -> {
                QuestionDTO questionDTO = new QuestionDTO();
                BeanUtils.copyProperties(questionDO, questionDTO);
                return questionDTO;
            })
            .collect(Collectors.toList());

        // 构建分页结果
        QuestionPageDTO pageDTO = new QuestionPageDTO();
        pageDTO.setList(questionDTOList);
        pageDTO.setTotal(total);
        pageDTO.setPageNum(queryDTO.getPageNum());
        pageDTO.setPageSize(queryDTO.getPageSize());
        pageDTO.setTotalPages((int) Math.ceil((double) total / queryDTO.getPageSize()));

        return pageDTO;
    }
}


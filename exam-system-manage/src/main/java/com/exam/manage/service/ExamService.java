package com.exam.manage.service;

import com.exam.manage.dal.dataobject.ExamDO;
import com.exam.manage.dal.dataobject.PaperDO;
import com.exam.manage.dal.mysqlmapper.ExamMapper;
import com.exam.manage.dal.mysqlmapper.PaperMapper;
import com.exam.manage.dto.ExamDTO;
import com.exam.manage.dto.ExamPageDTO;
import com.exam.manage.dto.ExamQueryDTO;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 考试安排服务类
 */
@Service
public class ExamService {

    @Autowired
    private ExamMapper examMapper;

    @Autowired
    private PaperMapper paperMapper;

    /**
     * 创建考试安排
     */
    @Transactional
    public ExamDTO createExam(ExamDTO examDTO) {
        // 1. 验证试卷是否存在
        PaperDO paper = paperMapper.selectById(examDTO.getPaperId());
        if (paper == null || paper.getStatus() == 0) {
            throw new RuntimeException("试卷不存在或已禁用");
        }

        // 2. 验证时间
        if (examDTO.getStartTime().isAfter(examDTO.getEndTime())) {
            throw new RuntimeException("开始时间不能晚于结束时间");
        }

        if (examDTO.getStartTime().isBefore(LocalDateTime.now())) {
            throw new RuntimeException("开始时间不能早于当前时间");
        }

        // 3. 计算考试时长（如果未提供）
        if (examDTO.getDuration() == null) {
            examDTO.setDuration(paper.getDuration());
        }

        // 4. 确定初始状态
        String status = determineExamStatus(examDTO.getStartTime(), examDTO.getEndTime());

        // 5. 创建考试安排
        ExamDO examDO = new ExamDO();
        BeanUtils.copyProperties(examDTO, examDO);
        examDO.setStatus(status);
        examDO.setAllowViewAnswer(examDTO.getAllowViewAnswer() != null ? examDTO.getAllowViewAnswer() : 0);
        examDO.setAllowRetake(examDTO.getAllowRetake() != null ? examDTO.getAllowRetake() : 0);
        examMapper.insert(examDO);

        // 6. 构建返回结果
        ExamDTO result = new ExamDTO();
        BeanUtils.copyProperties(examDO, result);
        return result;
    }

    /**
     * 根据ID查询考试安排
     */
    public ExamDTO getExamById(Long id) {
        ExamDO examDO = examMapper.selectById(id);
        if (examDO == null) {
            throw new RuntimeException("考试安排不存在");
        }

        ExamDTO examDTO = new ExamDTO();
        BeanUtils.copyProperties(examDO, examDTO);

        // 填充试卷信息
        PaperDO paper = paperMapper.selectById(examDO.getPaperId());
        if (paper != null) {
            examDTO.setPaperName(paper.getName());
        }

        return examDTO;
    }

    /**
     * 更新考试安排
     */
    @Transactional
    public ExamDTO updateExam(ExamDTO examDTO) {
        ExamDO existingExam = examMapper.selectById(examDTO.getId());
        if (existingExam == null) {
            throw new RuntimeException("考试安排不存在");
        }

        // 如果考试已开始或已结束，不允许修改
        if ("in_progress".equals(existingExam.getStatus()) || "finished".equals(existingExam.getStatus())) {
            throw new RuntimeException("考试已开始或已结束，不允许修改");
        }

        // 验证试卷
        if (examDTO.getPaperId() != null) {
            PaperDO paper = paperMapper.selectById(examDTO.getPaperId());
            if (paper == null || paper.getStatus() == 0) {
                throw new RuntimeException("试卷不存在或已禁用");
            }
        }

        // 验证时间
        if (examDTO.getStartTime() != null && examDTO.getEndTime() != null) {
            if (examDTO.getStartTime().isAfter(examDTO.getEndTime())) {
                throw new RuntimeException("开始时间不能晚于结束时间");
            }
        }

        // 更新考试安排
        ExamDO examDO = new ExamDO();
        BeanUtils.copyProperties(examDTO, examDO);
        
        // 重新确定状态
        if (examDTO.getStartTime() != null && examDTO.getEndTime() != null) {
            examDO.setStatus(determineExamStatus(examDTO.getStartTime(), examDTO.getEndTime()));
        } else {
            examDO.setStatus(existingExam.getStatus());
        }

        examMapper.update(examDO);
        return getExamById(examDTO.getId());
    }

    /**
     * 删除考试安排（取消考试）
     */
    @Transactional
    public void deleteExam(Long id) {
        ExamDO examDO = examMapper.selectById(id);
        if (examDO == null) {
            throw new RuntimeException("考试安排不存在");
        }

        // 如果考试已开始或已结束，不允许删除
        if ("in_progress".equals(examDO.getStatus()) || "finished".equals(examDO.getStatus())) {
            throw new RuntimeException("考试已开始或已结束，不允许删除");
        }

        examMapper.deleteById(id);
    }

    /**
     * 分页查询考试安排列表
     */
    public ExamPageDTO getExamList(ExamQueryDTO queryDTO) {
        // 计算偏移量
        Integer offset = (queryDTO.getPageNum() - 1) * queryDTO.getPageSize();

        // 查询列表
        List<ExamDO> examDOList = examMapper.selectList(
            queryDTO.getStatus(),
            queryDTO.getCreateUserId(),
            offset,
            queryDTO.getPageSize()
        );

        // 查询总数
        Long total = examMapper.count(
            queryDTO.getStatus(),
            queryDTO.getCreateUserId()
        );

        // 转换为DTO并填充关联信息
        List<ExamDTO> examDTOList = examDOList.stream()
            .map(examDO -> {
                ExamDTO examDTO = new ExamDTO();
                BeanUtils.copyProperties(examDO, examDTO);

                // 填充试卷信息
                PaperDO paper = paperMapper.selectById(examDO.getPaperId());
                if (paper != null) {
                    examDTO.setPaperName(paper.getName());
                }

                return examDTO;
            })
            .collect(Collectors.toList());

        // 构建分页结果
        ExamPageDTO pageDTO = new ExamPageDTO();
        pageDTO.setList(examDTOList);
        pageDTO.setTotal(total);
        pageDTO.setPageNum(queryDTO.getPageNum());
        pageDTO.setPageSize(queryDTO.getPageSize());
        pageDTO.setTotalPages((int) Math.ceil((double) total / queryDTO.getPageSize()));

        return pageDTO;
    }

    /**
     * 更新考试状态（根据时间）
     */
    @Transactional
    public void updateExamStatus(Long id) {
        ExamDO exam = examMapper.selectById(id);
        if (exam == null) {
            return;
        }

        String newStatus = determineExamStatus(exam.getStartTime(), exam.getEndTime());
        if (!newStatus.equals(exam.getStatus())) {
            examMapper.updateStatus(id, newStatus);
        }
    }

    /**
     * 批量更新考试状态（用于定时任务）
     */
    @Transactional
    public void batchUpdateExamStatus() {
        List<ExamDO> exams = examMapper.selectExamsNeedStatusUpdate();
        for (ExamDO exam : exams) {
            String newStatus = determineExamStatus(exam.getStartTime(), exam.getEndTime());
            if (!newStatus.equals(exam.getStatus())) {
                examMapper.updateStatus(exam.getId(), newStatus);
            }
        }
    }

    /**
     * 根据时间确定考试状态
     */
    private String determineExamStatus(LocalDateTime startTime, LocalDateTime endTime) {
        LocalDateTime now = LocalDateTime.now();
        if (now.isBefore(startTime)) {
            return "not_started";
        } else if (now.isAfter(endTime)) {
            return "finished";
        } else {
            return "in_progress";
        }
    }
}


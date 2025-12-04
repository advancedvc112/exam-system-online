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

    @Autowired
    private ExamTokenUtil examTokenUtil;

    @Autowired
    private org.springframework.data.redis.core.RedisTemplate<String, Object> redisTemplate;

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
     * 单场考试的状态切换（管理员/老师手工操作：“开启/结束考试”）
     * - not_started -> in_progress：立即开启考试（不再依赖开始时间）
     * - in_progress -> finished：手动结束考试
     * 其他状态不做处理
     * @return 如果开启了考试，返回考试令牌；否则返回null
     */
    @Transactional
    public String updateExamStatus(Long id) {
        ExamDO exam = examMapper.selectById(id);
        if (exam == null) {
            return null;
        }

        String currentStatus = exam.getStatus();
        String newStatus = null;
        if ("not_started".equals(currentStatus)) {
            newStatus = "in_progress";
        } else if ("in_progress".equals(currentStatus)) {
            newStatus = "finished";
        }

        if (newStatus != null && !newStatus.equals(currentStatus)) {
            examMapper.updateStatus(id, newStatus);
            
            // 当考试状态变为 in_progress 时，签发考试令牌
            if ("in_progress".equals(newStatus)) {
                String token = examTokenUtil.issueToken(id, exam.getEndTime());
                // 令牌已存储到Redis，学生可以通过 /exam-online/execute/token/{examId} 获取
                return token;
            }
        }
        return null;
    }

    /**
     * 批量更新考试状态（用于定时任务）
     * 当考试状态变为 finished 时，触发批量提交处理
     * 当考试状态变为 in_progress 时，生成考试令牌
     */
    @Transactional
    public void batchUpdateExamStatus() {
        List<ExamDO> exams = examMapper.selectExamsNeedStatusUpdate();
        for (ExamDO exam : exams) {
            String currentStatus = exam.getStatus();
            String newStatus = determineExamStatus(exam.getStartTime(), exam.getEndTime());
            if (!newStatus.equals(currentStatus)) {
                examMapper.updateStatus(exam.getId(), newStatus);
                
                // 当考试状态从 not_started 变为 in_progress 时，签发考试令牌
                if ("not_started".equals(currentStatus) && "in_progress".equals(newStatus)) {
                    examTokenUtil.issueToken(exam.getId(), exam.getEndTime());
                }
                
                // 当考试状态从 in_progress 变为 finished 时，触发批量提交处理
                if ("in_progress".equals(currentStatus) && "finished".equals(newStatus)) {
                    // 通知执行模块处理考试时间耗尽的批量提交
                    // 这里使用 Redis 发布订阅或直接调用服务
                    // 为了解耦，使用 Redis Set 标记需要处理的考试
                    // 实际处理由 ExamSubmitScheduler 定时任务执行
                    handleExamTimeout(exam.getId());
                }
            }
        }
    }

    /**
     * 处理考试时间耗尽（将考试加入待处理队列）
     * @param examId 考试ID
     */
    private void handleExamTimeout(Long examId) {
        // 使用 Redis Set 标记需要处理的考试
        // 实际处理由 ExamSubmitScheduler 定时任务执行
        // 这里通过 Redis 通知执行模块，避免跨模块直接依赖
        
        // 将考试ID加入待处理队列（执行模块的定时任务会处理）
        // Key: exam:timeout:exams (Set)
        redisTemplate.opsForSet().add("exam:timeout:exams", examId.toString());
        redisTemplate.expire("exam:timeout:exams", 24, java.util.concurrent.TimeUnit.HOURS);
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


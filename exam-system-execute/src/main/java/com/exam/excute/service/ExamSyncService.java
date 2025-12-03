package com.exam.excute.service;

import com.exam.excute.dal.dataobject.AnswerRecordDO;
import com.exam.excute.dal.dataobject.ExamRecordDO;
import com.exam.excute.dal.mysqlmapper.AnswerRecordMapper;
import com.exam.excute.dal.mysqlmapper.ExamRecordMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

/**
 * 考试实时同步服务类
 */
@Service
public class ExamSyncService {

    @Autowired
    private AnswerRecordMapper answerRecordMapper;

    @Autowired
    private ExamRecordMapper examRecordMapper;

    @Autowired
    private SimpMessagingTemplate messagingTemplate;

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    // Redis key前缀
    private static final String ANSWER_KEY_PREFIX = "exam:answer:";
    private static final String PROGRESS_KEY_PREFIX = "exam:progress:";

    /**
     * 学生开始一场考试：如果已有记录则直接返回，否则创建新的考试记录
     */
    public Long startExam(Long examId, Long studentId, Long paperId) {
        // 如果已经有记录，直接复用（防止重复开始）
        ExamRecordDO existing = examRecordMapper.selectByExamIdAndStudentId(examId, studentId);
        if (existing != null) {
            return existing.getId();
        }

        ExamRecordDO record = new ExamRecordDO();
        record.setExamId(examId);
        record.setStudentId(studentId);
        record.setPaperId(paperId);
        record.setStatus("in_progress");
        examRecordMapper.insert(record);
        return record.getId();
    }

    /**
     * 实时保存答案（先存Redis，异步同步到数据库）
     */
    public void saveAnswerRealtime(Long examRecordId, Long questionId, String studentAnswer) {
        String key = ANSWER_KEY_PREFIX + examRecordId + ":" + questionId;
        
        // 保存到Redis（实时）
        redisTemplate.opsForValue().set(key, studentAnswer, 2, TimeUnit.HOURS);
        
        // 查询是否已存在答题记录
        AnswerRecordDO existingRecord = answerRecordMapper.selectByExamRecordIdAndQuestionId(examRecordId, questionId);
        
        if (existingRecord != null) {
            // 更新现有记录
            existingRecord.setStudentAnswer(studentAnswer);
            answerRecordMapper.update(existingRecord);
        } else {
            // 创建新记录
            AnswerRecordDO answerRecord = new AnswerRecordDO();
            answerRecord.setExamRecordId(examRecordId);
            answerRecord.setQuestionId(questionId);
            answerRecord.setStudentAnswer(studentAnswer);
            answerRecord.setIsCorrect(0);
            answerRecord.setScore(0);
            answerRecord.setAnswerTime(System.currentTimeMillis());
            answerRecordMapper.insert(answerRecord);
        }
        
        // 更新进度
        updateProgress(examRecordId);
    }

    /**
     * 更新答题进度
     */
    public void updateProgress(Long examRecordId) {
        String key = PROGRESS_KEY_PREFIX + examRecordId;

        // 从Redis读取当前进度，兼容 Integer / Long 两种存储类型
        Object raw = redisTemplate.opsForValue().get(key);
        long answeredCount;
        if (raw instanceof Long) {
            answeredCount = (Long) raw;
        } else if (raw instanceof Integer) {
            answeredCount = ((Integer) raw).longValue();
        } else {
            answeredCount = 0L;
        }
        answeredCount = answeredCount + 1;

        // 保存进度到Redis
        redisTemplate.opsForValue().set(key, answeredCount, 2, TimeUnit.HOURS);

        // 通过WebSocket推送进度更新
        messagingTemplate.convertAndSend("/topic/exam/progress/" + examRecordId, answeredCount);
    }

    /**
     * 获取答题进度
     */
    public Long getProgress(Long examRecordId) {
        String key = PROGRESS_KEY_PREFIX + examRecordId;
        Object raw = redisTemplate.opsForValue().get(key);
        if (raw instanceof Long) {
            return (Long) raw;
        } else if (raw instanceof Integer) {
            return ((Integer) raw).longValue();
        }
        return 0L;
    }

    /**
     * 同步答案到数据库（批量）
     */
    public void syncAnswersToDatabase(Long examRecordId) {
        // 从Redis获取所有答案
        // 这里简化处理，实际应该遍历所有题目ID
        // 在提交时统一处理
    }

    /**
     * 推送考试状态更新
     */
    public void pushExamStatusUpdate(Long examId, String status) {
        messagingTemplate.convertAndSend("/topic/exam/status/" + examId, status);
    }

    /**
     * 推送警告消息
     */
    public void pushWarning(Long examRecordId, String message) {
        messagingTemplate.convertAndSend("/queue/exam/warning/" + examRecordId, message);
    }
}


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
    private ExamTokenService examTokenService;

    @Autowired
    private SimpMessagingTemplate messagingTemplate;

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    // Redis key前缀（加入令牌确保唯一性）
    private static final String ANSWER_KEY_PREFIX = "exam:answer:";  // 单个答案：exam:answer:{token}:{examRecordId}:{questionId}
    private static final String ANSWERED_QUESTIONS_PREFIX = "exam:answered:";  // 已答题题目Set：exam:answered:{token}:{examRecordId}（自动去重）
    private static final String PROGRESS_KEY_PREFIX = "exam:progress:";
    private static final String SYNC_QUEUE_PREFIX = "exam:sync:queue:";  // 待同步队列Set：exam:sync:queue:{token}:{examRecordId}（自动去重）
    private static final String SUBMIT_QUEUE_PREFIX = "exam:submit:queue:";  // 提交队列List：exam:submit:queue:{examId}（限流队列）

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
     * 实时保存答案（只写Redis，不立即写数据库）
     * 优化方案：
     * 1. 使用 Redis Set 存储已答题题目ID（自动去重，无需手动判断）
     * 2. 使用 String key-value 存储答案（简单高效）
     * 3. 使用 Set 维护待同步队列（自动去重，避免重复同步）
     * 4. 在key中加入考试令牌，确保每个考生的答案key唯一性
     * 5. 避免每次修改都触发数据库操作，大幅降低数据库压力
     * 
     * @param examRecordId 考试记录ID
     * @param questionId 题目ID
     * @param studentAnswer 学生答案
     * @param examToken 考试令牌（用于确保key唯一性）
     */
    public void saveAnswerRealtime(Long examRecordId, Long questionId, String studentAnswer, String examToken) {
        // 1. 保存答案（String 结构，key中包含令牌确保唯一性）
        // Key格式：exam:answer:{token}:{examRecordId}:{questionId}
        String answerKey = ANSWER_KEY_PREFIX + examToken + ":" + examRecordId + ":" + questionId;
        redisTemplate.opsForValue().set(answerKey, studentAnswer, 2, TimeUnit.HOURS);
        
        // 2. 将题目ID加入已答题Set（自动去重，多次保存同一题目不会重复）
        // Key格式：exam:answered:{token}:{examRecordId}
        String answeredQuestionsKey = ANSWERED_QUESTIONS_PREFIX + examToken + ":" + examRecordId;
        redisTemplate.opsForSet().add(answeredQuestionsKey, questionId.toString());
        redisTemplate.expire(answeredQuestionsKey, 2, TimeUnit.HOURS);
        
        // 3. 将题目ID加入待同步队列Set（自动去重，避免重复同步）
        // Key格式：exam:sync:queue:{token}:{examRecordId}
        String syncQueueKey = SYNC_QUEUE_PREFIX + examToken + ":" + examRecordId;
        redisTemplate.opsForSet().add(syncQueueKey, questionId.toString());
        redisTemplate.expire(syncQueueKey, 2, TimeUnit.HOURS);
        
        // 4. 更新进度（使用Set大小，自动去重统计）
        updateProgress(examRecordId, examToken);
    }

    /**
     * 更新答题进度（使用Set大小统计，自动去重，原子操作）
     * @param examRecordId 考试记录ID
     * @param examToken 考试令牌（用于确保key唯一性）
     */
    public void updateProgress(Long examRecordId, String examToken) {
        // 使用Set大小统计已答题数量（自动去重，准确且高效）
        // Key格式：exam:answered:{token}:{examRecordId}
        String answeredQuestionsKey = ANSWERED_QUESTIONS_PREFIX + examToken + ":" + examRecordId;
        Long answeredCount = redisTemplate.opsForSet().size(answeredQuestionsKey);
        
        if (answeredCount == null) {
            answeredCount = 0L;
        }
        
        // 更新进度计数器（用于快速查询，避免每次都计算Set大小）
        String progressKey = PROGRESS_KEY_PREFIX + examRecordId;
        redisTemplate.opsForValue().set(progressKey, answeredCount, 2, TimeUnit.HOURS);

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
     * 从Redis获取答案（直接使用String key获取，简单高效）
     * @param examRecordId 考试记录ID
     * @param questionId 题目ID
     * @param examToken 考试令牌（用于确保key唯一性）
     */
    public String getAnswerFromRedis(Long examRecordId, Long questionId, String examToken) {
        // Key格式：exam:answer:{token}:{examRecordId}:{questionId}
        String answerKey = ANSWER_KEY_PREFIX + examToken + ":" + examRecordId + ":" + questionId;
        Object answer = redisTemplate.opsForValue().get(answerKey);
        return answer != null ? answer.toString() : null;
    }

    /**
     * 获取所有已答题的题目ID（使用Set，自动去重）
     * @param examRecordId 考试记录ID
     * @param examToken 考试令牌（用于确保key唯一性）
     */
    public java.util.Set<String> getAnsweredQuestionIds(Long examRecordId, String examToken) {
        // Key格式：exam:answered:{token}:{examRecordId}
        String answeredQuestionsKey = ANSWERED_QUESTIONS_PREFIX + examToken + ":" + examRecordId;
        java.util.Set<Object> questionIdsObj = (java.util.Set<Object>) redisTemplate.opsForSet().members(answeredQuestionsKey);
        if (questionIdsObj == null || questionIdsObj.isEmpty()) {
            return java.util.Collections.emptySet();
        }
        // 转换为String Set
        java.util.Set<String> questionIds = new java.util.HashSet<>();
        for (Object obj : questionIdsObj) {
            questionIds.add(obj.toString());
        }
        return questionIds;
    }

    /**
     * 批量同步答案到数据库（定时任务调用）
     * 优化：使用Set自动去重，批量操作，减少数据库交互次数
     * @param examRecordId 考试记录ID
     * @param examToken 考试令牌（用于确保key唯一性）
     */
    public void syncAnswersToDatabase(Long examRecordId, String examToken) {
        // Key格式：exam:sync:queue:{token}:{examRecordId}
        String syncQueueKey = SYNC_QUEUE_PREFIX + examToken + ":" + examRecordId;
        
        // 获取待同步的题目ID列表（Set自动去重）
        java.util.Set<Object> questionIdSetObj = (java.util.Set<Object>) redisTemplate.opsForSet().members(syncQueueKey);
        if (questionIdSetObj == null || questionIdSetObj.isEmpty()) {
            return;
        }
        // 转换为String Set
        java.util.Set<String> questionIdSet = new java.util.HashSet<>();
        for (Object obj : questionIdSetObj) {
            questionIdSet.add(obj.toString());
        }
        
        java.util.List<AnswerRecordDO> toInsert = new java.util.ArrayList<>();
        java.util.List<AnswerRecordDO> toUpdate = new java.util.ArrayList<>();
        
        // 从Redis批量获取答案
        for (String questionIdStr : questionIdSet) {
            Long questionId = Long.parseLong(questionIdStr);
            String studentAnswer = getAnswerFromRedis(examRecordId, questionId, examToken);
            if (studentAnswer == null) {
                continue;
            }
            
            // 检查数据库中是否已存在
            AnswerRecordDO existing = answerRecordMapper.selectByExamRecordIdAndQuestionId(examRecordId, questionId);
            
            if (existing != null) {
                // 只有答案发生变化时才更新
                if (!studentAnswer.equals(existing.getStudentAnswer())) {
                    existing.setStudentAnswer(studentAnswer);
                    existing.setAnswerTime(System.currentTimeMillis());
                    toUpdate.add(existing);
                }
            } else {
                // 新答案，需要插入
                AnswerRecordDO answerRecord = new AnswerRecordDO();
                answerRecord.setExamRecordId(examRecordId);
                answerRecord.setQuestionId(questionId);
                answerRecord.setStudentAnswer(studentAnswer);
                answerRecord.setIsCorrect(0);
                answerRecord.setScore(0);
                answerRecord.setAnswerTime(System.currentTimeMillis());
                toInsert.add(answerRecord);
            }
        }
        
        // 批量更新
        if (!toUpdate.isEmpty()) {
            for (AnswerRecordDO record : toUpdate) {
                answerRecordMapper.update(record);
            }
        }
        
        // 批量插入
        if (!toInsert.isEmpty()) {
            answerRecordMapper.batchInsert(toInsert);
        }
        
        // 清空同步队列（Set已自动去重，清空后等待下次添加）
        redisTemplate.delete(syncQueueKey);
    }

    /**
     * 强制同步所有答案到数据库（考试提交时调用）
     * 使用Set获取已答题列表，自动去重
     * @param examRecordId 考试记录ID
     * @param examToken 考试令牌（用于确保key唯一性）
     */
    public void forceSyncAllAnswersToDatabase(Long examRecordId, String examToken) {
        // 从Set获取所有已答题的题目ID（自动去重）
        java.util.Set<String> questionIdSet = getAnsweredQuestionIds(examRecordId, examToken);
        if (questionIdSet.isEmpty()) {
            return;
        }
        
        java.util.List<AnswerRecordDO> toInsert = new java.util.ArrayList<>();
        java.util.List<AnswerRecordDO> toUpdate = new java.util.ArrayList<>();
        
        // 批量获取答案并同步
        for (String questionIdStr : questionIdSet) {
            Long questionId = Long.parseLong(questionIdStr);
            String studentAnswer = getAnswerFromRedis(examRecordId, questionId, examToken);
            if (studentAnswer == null) {
                continue;
            }
            
            AnswerRecordDO existing = answerRecordMapper.selectByExamRecordIdAndQuestionId(examRecordId, questionId);
            
            if (existing != null) {
                existing.setStudentAnswer(studentAnswer);
                existing.setAnswerTime(System.currentTimeMillis());
                toUpdate.add(existing);
            } else {
                AnswerRecordDO answerRecord = new AnswerRecordDO();
                answerRecord.setExamRecordId(examRecordId);
                answerRecord.setQuestionId(questionId);
                answerRecord.setStudentAnswer(studentAnswer);
                answerRecord.setIsCorrect(0);
                answerRecord.setScore(0);
                answerRecord.setAnswerTime(System.currentTimeMillis());
                toInsert.add(answerRecord);
            }
        }
        
        // 批量更新
        if (!toUpdate.isEmpty()) {
            for (AnswerRecordDO record : toUpdate) {
                answerRecordMapper.update(record);
            }
        }
        
        // 批量插入
        if (!toInsert.isEmpty()) {
            answerRecordMapper.batchInsert(toInsert);
        }
        
        // 清理 Redis 数据（可选，根据业务需求决定是否立即清理）
        // String answeredQuestionsKey = ANSWERED_QUESTIONS_PREFIX + examRecordId;
        // redisTemplate.delete(answeredQuestionsKey);
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

    /**
     * 考生提前结束考试：直接提交答案到数据库，清除该考生的考试令牌
     * @param examRecordId 考试记录ID
     * @param examToken 考试令牌
     * @param examId 考试ID
     */
    public void submitExamEarly(Long examRecordId, String examToken, Long examId) {
        // 1. 强制同步所有答案到数据库
        forceSyncAllAnswersToDatabase(examRecordId, examToken);
        
        // 2. 更新考试记录状态为已提交
        ExamRecordDO record = examRecordMapper.selectById(examRecordId);
        if (record != null && "in_progress".equals(record.getStatus())) {
            record.setStatus("submitted");
            record.setSubmitTime(java.time.LocalDateTime.now());
            examRecordMapper.update(record);
        }
        
        // 3. 清理该考生的Redis数据
        cleanupStudentRedisData(examRecordId, examToken);
        
        // 注意：提前结束不清除考试令牌，因为其他考生可能还在考试
        // 考试令牌在考试时间耗尽时统一清除
    }

    /**
     * 考试时间耗尽：将考生加入提交队列，使用限流机制批量提交
     * @param examId 考试ID
     */
    public void handleExamTimeout(Long examId) {
        // 1. 将考试ID加入待处理队列（由定时任务限流处理）
        // 使用 Set 避免重复添加
        redisTemplate.opsForSet().add("exam:timeout:exams", examId.toString());
        redisTemplate.expire("exam:timeout:exams", 24, TimeUnit.HOURS);
        
        // 2. 立即处理一次（将考生加入提交队列）
        processTimeoutExamSubmission(examId);
    }

    /**
     * 处理考试时间耗尽的批量提交（限流处理）
     * @param examId 考试ID
     */
    public void processTimeoutExamSubmission(Long examId) {
        // 1. 获取考试令牌
        String examToken = examTokenService.getToken(examId);
        if (examToken == null) {
            return; // 令牌已清除，说明已经处理过
        }
        
        // 2. 查询该考试的所有进行中的考试记录
        java.util.List<ExamRecordDO> records = examRecordMapper.selectInProgressByExamId(examId);
        if (records == null || records.isEmpty()) {
            // 没有需要提交的记录，直接清除令牌
            examTokenService.revokeToken(examId);
            return;
        }
        
        // 3. 将每个考生的 examRecordId 加入提交队列（限流队列）
        for (ExamRecordDO record : records) {
            addToSubmitQueue(examId, record.getId(), examToken);
        }
    }

    /**
     * 将考生加入提交队列（限流队列）
     * @param examId 考试ID
     * @param examRecordId 考试记录ID
     * @param examToken 考试令牌
     */
    public void addToSubmitQueue(Long examId, Long examRecordId, String examToken) {
        String submitQueueKey = SUBMIT_QUEUE_PREFIX + examId;
        // 使用 List 作为队列，格式：{examRecordId}:{examToken}
        String queueItem = examRecordId + ":" + examToken;
        redisTemplate.opsForList().rightPush(submitQueueKey, queueItem);
        redisTemplate.expire(submitQueueKey, 24, TimeUnit.HOURS);
    }

    /**
     * 从提交队列中取出并处理（限流处理，每次处理一定数量）
     * @param examId 考试ID
     * @param batchSize 每次处理的批次大小（限流）
     */
    public void processSubmitQueue(Long examId, int batchSize) {
        String submitQueueKey = SUBMIT_QUEUE_PREFIX + examId;
        
        for (int i = 0; i < batchSize; i++) {
            // 从队列左侧取出（FIFO）
            Object item = redisTemplate.opsForList().leftPop(submitQueueKey);
            if (item == null) {
                break; // 队列为空
            }
            
            try {
                String[] parts = item.toString().split(":", 2);
                if (parts.length != 2) {
                    continue;
                }
                
                Long examRecordId = Long.parseLong(parts[0]);
                String examToken = parts[1];
                
                // 提交该考生的答案
                submitStudentExam(examRecordId, examToken);
            } catch (Exception e) {
                // 记录日志，继续处理下一个
                System.err.println("处理提交队列失败: " + item + ", 错误: " + e.getMessage());
            }
        }
        
    }

    /**
     * 检查队列是否为空，如果为空则清除考试令牌
     * @param examId 考试ID
     */
    public void revokeExamTokenIfQueueEmpty(Long examId) {
        String submitQueueKey = SUBMIT_QUEUE_PREFIX + examId;
        Long queueSize = redisTemplate.opsForList().size(submitQueueKey);
        if (queueSize == null || queueSize == 0) {
            // 队列为空，所有考生都已提交，清除考试令牌
            examTokenService.revokeToken(examId);
        }
    }

    /**
     * 提交单个考生的考试（从队列中取出后执行）
     * @param examRecordId 考试记录ID
     * @param examToken 考试令牌
     */
    private void submitStudentExam(Long examRecordId, String examToken) {
        // 1. 强制同步所有答案到数据库
        forceSyncAllAnswersToDatabase(examRecordId, examToken);
        
        // 2. 更新考试记录状态为已提交
        ExamRecordDO record = examRecordMapper.selectById(examRecordId);
        if (record != null && "in_progress".equals(record.getStatus())) {
            record.setStatus("submitted");
            record.setSubmitTime(java.time.LocalDateTime.now());
            examRecordMapper.update(record);
        }
        
        // 3. 清理该考生的Redis数据
        cleanupStudentRedisData(examRecordId, examToken);
    }

    /**
     * 清理考生的Redis数据
     * @param examRecordId 考试记录ID
     * @param examToken 考试令牌
     */
    private void cleanupStudentRedisData(Long examRecordId, String examToken) {
        // 清理答案相关的Redis数据
        String answeredQuestionsKey = ANSWERED_QUESTIONS_PREFIX + examToken + ":" + examRecordId;
        redisTemplate.delete(answeredQuestionsKey);
        
        String syncQueueKey = SYNC_QUEUE_PREFIX + examToken + ":" + examRecordId;
        redisTemplate.delete(syncQueueKey);
        
        // 清理进度计数器
        String progressKey = PROGRESS_KEY_PREFIX + examRecordId;
        redisTemplate.delete(progressKey);
        
        // 清理所有答案key（需要扫描，这里简化处理）
        // 实际可以使用 SCAN 命令遍历所有相关的 key
    }
}


package com.exam.excute.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.Set;
import java.util.concurrent.TimeUnit;

/**
 * 考试提交定时任务
 * 处理考试时间耗尽的批量提交，使用Redis队列限流
 */
@Component
public class ExamSubmitScheduler {

    @Autowired
    private ExamSyncService examSyncService;

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    private static final String SUBMIT_QUEUE_PREFIX = "exam:submit:queue:";
    private static final String TIMEOUT_EXAMS_KEY = "exam:timeout:exams";
    
    // 每次处理的批次大小（限流：每次最多处理10个考生）
    private static final int BATCH_SIZE = 10;

    /**
     * 每10秒执行一次，处理提交队列（限流处理）
     * 优化：可以根据实际负载调整处理频率和批次大小
     */
    @Scheduled(fixedDelay = 10000) // 10秒
    public void processSubmitQueues() {
        // 获取所有待处理的考试ID
        Set<Object> examIds = redisTemplate.opsForSet().members(TIMEOUT_EXAMS_KEY);
        if (examIds == null || examIds.isEmpty()) {
            return;
        }

        for (Object examIdObj : examIds) {
            try {
                Long examId = Long.parseLong(examIdObj.toString());
                String submitQueueKey = SUBMIT_QUEUE_PREFIX + examId;
                
                // 检查队列是否还有待处理的考生
                Long queueSize = redisTemplate.opsForList().size(submitQueueKey);
                if (queueSize == null || queueSize == 0) {
                    // 队列为空，检查是否所有考生都已处理完成，清除考试令牌
                    examSyncService.revokeExamTokenIfQueueEmpty(examId);
                    
                    // 从待处理列表中移除
                    redisTemplate.opsForSet().remove(TIMEOUT_EXAMS_KEY, examIdObj);
                    continue;
                }
                
                // 限流处理：每次只处理一定数量的考生
                examSyncService.processSubmitQueue(examId, BATCH_SIZE);
            } catch (Exception e) {
                // 记录日志，继续处理下一个
                System.err.println("处理提交队列失败: " + examIdObj + ", 错误: " + e.getMessage());
            }
        }
    }

    /**
     * 每30秒执行一次，检查是否有新的考试时间耗尽，初始化提交队列
     */
    @Scheduled(fixedDelay = 30000) // 30秒
    public void initSubmitQueues() {
        // 获取所有待处理的考试ID
        Set<Object> examIds = redisTemplate.opsForSet().members(TIMEOUT_EXAMS_KEY);
        if (examIds == null || examIds.isEmpty()) {
            return;
        }

        for (Object examIdObj : examIds) {
            try {
                Long examId = Long.parseLong(examIdObj.toString());
                String submitQueueKey = SUBMIT_QUEUE_PREFIX + examId;
                
                // 检查队列是否已初始化
                Long queueSize = redisTemplate.opsForList().size(submitQueueKey);
                if (queueSize == null || queueSize == 0) {
                    // 队列为空，可能是新加入的考试，初始化提交队列
                    examSyncService.processTimeoutExamSubmission(examId);
                }
            } catch (Exception e) {
                // 记录日志，继续处理下一个
                System.err.println("初始化提交队列失败: " + examIdObj + ", 错误: " + e.getMessage());
            }
        }
    }

    /**
     * 每小时清理过期的提交队列（防止内存泄漏）
     */
    @Scheduled(fixedDelay = 3600000) // 1小时
    public void cleanupExpiredSubmitQueues() {
        Set<String> keys = redisTemplate.keys(SUBMIT_QUEUE_PREFIX + "*");
        if (keys == null || keys.isEmpty()) {
            return;
        }

        for (String key : keys) {
            // 检查 key 是否还有 TTL，如果没有 TTL 或已过期，删除
            Long ttl = redisTemplate.getExpire(key, TimeUnit.SECONDS);
            if (ttl == null || ttl <= 0) {
                redisTemplate.delete(key);
            }
        }
    }
}


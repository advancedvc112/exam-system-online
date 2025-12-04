package com.exam.excute.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.Set;
import java.util.concurrent.TimeUnit;

/**
 * 答案同步定时任务
 * 定期将 Redis 中的答案批量同步到数据库，减少数据库压力
 */
@Component
public class AnswerSyncScheduler {

    @Autowired
    private ExamSyncService examSyncService;

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    private static final String SYNC_QUEUE_PREFIX = "exam:sync:queue:";  // 格式：exam:sync:queue:{token}:{examRecordId}

    /**
     * 每30秒执行一次批量同步
     * 优化：可以根据实际负载调整同步频率
     */
    @Scheduled(fixedDelay = 30000) // 30秒
    public void syncAnswersPeriodically() {
        // 查找所有待同步的考试记录
        // 注意：这里简化处理，实际应该维护一个待同步列表
        // 可以通过 Redis Set 或 List 来维护需要同步的 examRecordId 列表
        
        // 方案1：扫描所有 sync queue（性能较低，但实现简单）
        // 方案2：维护一个待同步列表（推荐，但需要额外维护）
        
        // 这里使用方案1，实际生产环境建议使用方案2
        Set<String> keys = redisTemplate.keys(SYNC_QUEUE_PREFIX + "*");
        if (keys == null || keys.isEmpty()) {
            return;
        }

        for (String key : keys) {
            try {
                // 从 key 中提取 token 和 examRecordId
                // key 格式：exam:sync:queue:{token}:{examRecordId}
                String suffix = key.substring(SYNC_QUEUE_PREFIX.length());
                String[] parts = suffix.split(":", 2);
                if (parts.length != 2) {
                    continue; // 格式不正确，跳过
                }
                String examToken = parts[0];
                Long examRecordId = Long.parseLong(parts[1]);
                
                // 批量同步答案
                examSyncService.syncAnswersToDatabase(examRecordId, examToken);
            } catch (Exception e) {
                // 记录日志，继续处理下一个
                System.err.println("同步答案失败: " + key + ", 错误: " + e.getMessage());
            }
        }
    }

    /**
     * 每小时清理过期的同步队列（防止内存泄漏）
     */
    @Scheduled(fixedDelay = 3600000) // 1小时
    public void cleanupExpiredSyncQueues() {
        Set<String> keys = redisTemplate.keys(SYNC_QUEUE_PREFIX + "*");
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


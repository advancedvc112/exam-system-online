package com.exam.manage.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * 考试令牌工具类（用于管理模块签发令牌）
 * 与执行模块的 ExamTokenService 使用相同的 Redis key 格式
 */
@Component
public class ExamTokenUtil {

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    // Redis key前缀（与执行模块保持一致）
    private static final String TOKEN_KEY_PREFIX = "exam:token:";

    /**
     * 为考试签发令牌
     * @param examId 考试ID
     * @param endTime 考试结束时间（用于设置令牌过期时间）
     * @return 考试令牌
     */
    public String issueToken(Long examId, LocalDateTime endTime) {
        // 生成唯一令牌
        String token = UUID.randomUUID().toString().replace("-", "");
        
        // 计算过期时间（考试结束时间 + 1小时缓冲，确保考试结束后还能验证）
        LocalDateTime expireTime = endTime.plusHours(1);
        Duration duration = Duration.between(LocalDateTime.now(), expireTime);
        long seconds = duration.getSeconds();
        
        // 如果已经过期，设置最小过期时间为1小时
        if (seconds <= 0) {
            seconds = 3600;
        }
        
        // 存储到Redis，key格式：exam:token:{examId}
        String key = TOKEN_KEY_PREFIX + examId;
        redisTemplate.opsForValue().set(key, token, seconds, TimeUnit.SECONDS);
        
        return token;
    }
}


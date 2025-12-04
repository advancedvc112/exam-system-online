package com.exam.excute.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * 考试令牌服务类
 * 用于高并发场景下的考试令牌签发和验证
 */
@Service
public class ExamTokenService {

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    // Redis key前缀
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

    /**
     * 验证考试令牌
     * @param examId 考试ID
     * @param token 令牌
     * @return 是否有效
     */
    public boolean validateToken(Long examId, String token) {
        if (examId == null || token == null || token.trim().isEmpty()) {
            return false;
        }
        
        String key = TOKEN_KEY_PREFIX + examId;
        Object storedToken = redisTemplate.opsForValue().get(key);
        
        if (storedToken == null) {
            return false;
        }
        
        return token.equals(storedToken.toString());
    }

    /**
     * 删除考试令牌（考试结束时调用）
     * @param examId 考试ID
     */
    public void revokeToken(Long examId) {
        String key = TOKEN_KEY_PREFIX + examId;
        redisTemplate.delete(key);
    }

    /**
     * 检查考试是否有有效令牌
     * @param examId 考试ID
     * @return 是否有有效令牌
     */
    public boolean hasToken(Long examId) {
        String key = TOKEN_KEY_PREFIX + examId;
        return Boolean.TRUE.equals(redisTemplate.hasKey(key));
    }

    /**
     * 获取考试令牌（用于学生获取令牌进入考试）
     * @param examId 考试ID
     * @return 考试令牌，如果不存在则返回null
     */
    public String getToken(Long examId) {
        String key = TOKEN_KEY_PREFIX + examId;
        Object token = redisTemplate.opsForValue().get(key);
        return token != null ? token.toString() : null;
    }
}


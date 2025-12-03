package com.exam.excute.service;

import com.exam.excute.dal.mysqlmapper.ExamRecordMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

/**
 * 防作弊服务类
 */
@Service
public class AntiCheatService {

    @Autowired
    private ExamRecordMapper examRecordMapper;

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    // Redis key前缀
    private static final String SWITCH_KEY_PREFIX = "exam:switch:";
    private static final String HEARTBEAT_KEY_PREFIX = "exam:heartbeat:";
    private static final String FOCUS_KEY_PREFIX = "exam:focus:";

    // 切屏阈值
    private static final int MAX_SWITCH_COUNT = 5;
    // 心跳超时时间（秒）
    private static final long HEARTBEAT_TIMEOUT = 30;
    // 失焦超时时间（秒）
    private static final long FOCUS_TIMEOUT = 10;

    /**
     * 记录切屏事件
     */
    public void recordSwitch(Long examRecordId, Long studentId) {
        String key = SWITCH_KEY_PREFIX + examRecordId + ":" + studentId;
        
        // 增加切屏次数
        examRecordMapper.incrementSwitchCount(examRecordId);
        
        // 记录到Redis（用于实时监控）
        Integer count = (Integer) redisTemplate.opsForValue().get(key);
        if (count == null) {
            count = 0;
        }
        redisTemplate.opsForValue().set(key, count + 1, 2, TimeUnit.HOURS);
        
        // 如果超过阈值，标记为作弊
        if (count + 1 >= MAX_SWITCH_COUNT) {
            markCheating(examRecordId, "切屏次数超过限制（" + (count + 1) + "次）");
        }
    }

    /**
     * 记录心跳
     */
    public void recordHeartbeat(Long examRecordId, Long studentId) {
        String key = HEARTBEAT_KEY_PREFIX + examRecordId + ":" + studentId;
        redisTemplate.opsForValue().set(key, System.currentTimeMillis(), HEARTBEAT_TIMEOUT, TimeUnit.SECONDS);
    }

    /**
     * 检查心跳是否超时
     */
    public boolean isHeartbeatTimeout(Long examRecordId, Long studentId) {
        String key = HEARTBEAT_KEY_PREFIX + examRecordId + ":" + studentId;
        Long lastHeartbeat = (Long) redisTemplate.opsForValue().get(key);
        if (lastHeartbeat == null) {
            return true;
        }
        return System.currentTimeMillis() - lastHeartbeat > HEARTBEAT_TIMEOUT * 1000;
    }

    /**
     * 记录窗口失焦
     */
    public void recordBlur(Long examRecordId, Long studentId) {
        String key = FOCUS_KEY_PREFIX + examRecordId + ":" + studentId;
        redisTemplate.opsForValue().set(key, System.currentTimeMillis(), FOCUS_TIMEOUT, TimeUnit.SECONDS);
    }

    /**
     * 记录窗口聚焦
     */
    public void recordFocus(Long examRecordId, Long studentId) {
        String key = FOCUS_KEY_PREFIX + examRecordId + ":" + studentId;
        Long blurTime = (Long) redisTemplate.opsForValue().get(key);
        if (blurTime != null) {
            long blurDuration = System.currentTimeMillis() - blurTime;
            // 如果失焦时间超过阈值，记录为切屏
            if (blurDuration > FOCUS_TIMEOUT * 1000) {
                recordSwitch(examRecordId, studentId);
            }
            redisTemplate.delete(key);
        }
    }

    /**
     * 标记作弊
     */
    public void markCheating(Long examRecordId, String reason) {
        examRecordMapper.markCheating(examRecordId, reason);
    }

    /**
     * 检测异常行为
     */
    public void detectAbnormalBehavior(Long examRecordId, Long studentId) {
        // 检查心跳超时
        if (isHeartbeatTimeout(examRecordId, studentId)) {
            markCheating(examRecordId, "心跳超时，可能离开考试页面");
        }
    }
}


package com.exam.excute.util;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * 分布式锁工具类
 * 基于 Redis 实现分布式锁，用于防止高并发场景下的重复操作
 */
@Component
public class DistributedLockUtil {

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    // Lua脚本：释放锁（确保只释放自己持有的锁）
    private static final String UNLOCK_SCRIPT = 
        "if redis.call('get', KEYS[1]) == ARGV[1] then " +
        "    return redis.call('del', KEYS[1]) " +
        "else " +
        "    return 0 " +
        "end";

    /**
     * 尝试获取分布式锁
     * @param lockKey 锁的key
     * @param timeout 锁的超时时间（秒）
     * @return 锁的value，如果获取失败返回null
     */
    public String tryLock(String lockKey, long timeout) {
        String lockValue = UUID.randomUUID().toString();
        Boolean success = redisTemplate.opsForValue()
            .setIfAbsent(lockKey, lockValue, timeout, TimeUnit.SECONDS);
        
        if (Boolean.TRUE.equals(success)) {
            return lockValue;
        }
        return null;
    }

    /**
     * 尝试获取分布式锁（带等待时间）
     * @param lockKey 锁的key
     * @param timeout 锁的超时时间（秒）
     * @param waitTime 等待获取锁的最大时间（毫秒）
     * @return 锁的value，如果获取失败返回null
     */
    public String tryLockWithWait(String lockKey, long timeout, long waitTime) {
        long endTime = System.currentTimeMillis() + waitTime;
        String lockValue = null;
        
        while (System.currentTimeMillis() < endTime) {
            lockValue = tryLock(lockKey, timeout);
            if (lockValue != null) {
                return lockValue;
            }
            
            // 短暂休眠后重试
            try {
                Thread.sleep(50); // 50ms
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return null;
            }
        }
        
        return null;
    }

    /**
     * 释放分布式锁
     * @param lockKey 锁的key
     * @param lockValue 锁的value（必须与获取锁时返回的value一致）
     * @return 是否释放成功
     */
    public boolean releaseLock(String lockKey, String lockValue) {
        if (lockKey == null || lockValue == null) {
            return false;
        }
        
        DefaultRedisScript<Long> script = new DefaultRedisScript<>();
        script.setScriptText(UNLOCK_SCRIPT);
        script.setResultType(Long.class);
        
        Long result = redisTemplate.execute(script, 
            Collections.singletonList(lockKey), 
            lockValue);
        
        return result != null && result > 0;
    }

    /**
     * 执行带锁的操作
     * @param lockKey 锁的key
     * @param timeout 锁的超时时间（秒）
     * @param waitTime 等待获取锁的最大时间（毫秒）
     * @param action 需要执行的操作
     * @param <T> 返回值类型
     * @return 操作结果
     * @throws RuntimeException 如果获取锁失败或操作异常
     */
    public <T> T executeWithLock(String lockKey, long timeout, long waitTime, LockAction<T> action) {
        String lockValue = tryLockWithWait(lockKey, timeout, waitTime);
        if (lockValue == null) {
            throw new RuntimeException("获取分布式锁失败，请稍后重试");
        }
        
        try {
            return action.execute();
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException("执行操作时发生异常", e);
        } finally {
            releaseLock(lockKey, lockValue);
        }
    }

    /**
     * 执行带锁的操作（快速失败，不等待）
     * @param lockKey 锁的key
     * @param timeout 锁的超时时间（秒）
     * @param action 需要执行的操作
     * @param <T> 返回值类型
     * @return 操作结果
     * @throws RuntimeException 如果获取锁失败或操作异常
     */
    public <T> T executeWithLock(String lockKey, long timeout, LockAction<T> action) {
        String lockValue = tryLock(lockKey, timeout);
        if (lockValue == null) {
            throw new RuntimeException("获取分布式锁失败，操作可能正在进行中，请稍后重试");
        }
        
        try {
            return action.execute();
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException("执行操作时发生异常", e);
        } finally {
            releaseLock(lockKey, lockValue);
        }
    }

    /**
     * 锁操作接口
     */
    @FunctionalInterface
    public interface LockAction<T> {
        T execute() throws Exception;
    }
}


package com.wechat.www.manager;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;

/**
 * 固定窗口限流计数。
 *
 * <p>用 String 模板而不是 {@code RedisTemplate<String,Object>}：{@code INCR} 要求 value 是裸数字字节，
 * JSON 序列化写出的 {@code "0"} 带引号会直接报错。
 */
@Component
@RequiredArgsConstructor
public class RateLimiter {

  private final StringRedisTemplate stringRedisTemplate;

  /**
   * 命中一次并返回窗口内累计次数。
   *
   * <p>窗口在首次命中时由 setIfAbsent 一次性写入值与 TTL，之后 INCR 只改值不动 TTL，
   * 不存在"有值无过期"的窗口；持续请求也不续期，窗口从首次请求起算。
   */
  public long hit(String key, Duration window) {
    stringRedisTemplate.opsForValue().setIfAbsent(key, "0", window);
    Long count = stringRedisTemplate.opsForValue().increment(key);
    return count == null ? 0L : count;
  }

}

package com.wechat.www.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.RedisSerializer;

/**
 * Redis 模板配置：在 StringRedisTemplate 之外，额外提供一个 value 走 JSON 的模板。
 *
 * <p>两者分工明确，必须并存，不能互相替代：
 * <ul>
 *   <li>{@code StringRedisTemplate} —— 计数器、验证码、频控占位等纯字符串场景。
 *       项目里 {@code INCR} 依赖 value 是裸数字字节，而 JSON 序列化写出的 {@code "0"}
 *       带引号，INCR 会直接报 {@code ERR value is not an integer or out of range}，
 *       所以这些 key 只能用 String 序列化。</li>
 *   <li>本模板 {@code RedisTemplate<String, Object>} —— 存 POJO / 结构体时使用，
 *       value 用 JSON，Redis 里人可读。</li>
 * </ul>
 *
 * <p>两个模板的 key 序列化器一致（都是 String），同一把 key 两边都能识别；
 * 但 value 序列化方式不同，同一个 key 不要混用两种模板写。
 */
@Configuration
public class RedisConfig {

  /**
   * 自定义同名 bean 后，Spring Boot 自动配置的 {@code RedisTemplate<Object, Object>} 会退让，
   * 不会产生 bean 冲突；{@code StringRedisTemplate} 不受影响，照常自动装配。
   */
  @Bean
  public RedisTemplate<String, Object> redisTemplate(RedisConnectionFactory connectionFactory) {
    RedisTemplate<String, Object> template = new RedisTemplate<>();
    template.setConnectionFactory(connectionFactory);
    template.setKeySerializer(RedisSerializer.string());
    template.setHashKeySerializer(RedisSerializer.string());
    // JSON 序列化会写入 @class 类型信息，存 POJO 时反序列化才能还原成原类型
    template.setValueSerializer(RedisSerializer.json());
    template.setHashValueSerializer(RedisSerializer.json());
    return template;
  }

}

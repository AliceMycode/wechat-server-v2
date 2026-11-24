package com.wechat.www.manager;

import com.wechat.www.common.constant.RedisKeyConst;
import com.wechat.www.common.security.LoginUser;
import com.wechat.www.config.AuthProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.security.SecureRandom;
import java.time.Duration;
import java.util.Base64;
import java.util.Map;

/**
 * 登录会话管理：opaque token 与 Redis 会话的读写。
 *
 * <p>token 本身不携带任何信息，只是一个 32 字节的随机串，
 * 所有身份信息都在 Redis 里，因此可以随时删除会话实现"立即下线"，
 * 这是纯 JWT 做不到的。
 *
 * <p>两个 key 同生命周期：
 * <ul>
 *   <li>{@code wechat:auth:session:{token}} —— 会话本身，鉴权时读取</li>
 *   <li>{@code wechat:auth:user:{userId}:{deviceId}} —— 设备到 token 的索引，
 *       同一设备重复登录时用它找到旧会话并踢掉</li>
 * </ul>
 */
@Component
@RequiredArgsConstructor
public class TokenManager {

  /**
   * 会话 Hash 的字段名
   */
  private static final String FIELD_USER_ID = "userId";

  /**
   * 会话 Hash 的字段名
   */
  private static final String FIELD_DEVICE_ID = "deviceId";

  /**
   * token 随机字节数：256 位熵，暴力猜中概率可忽略
   */
  private static final int TOKEN_BYTES = 32;

  /**
   * SecureRandom 线程安全，可复用同一个实例，避免每次生成都重新播种
   */
  private static final SecureRandom SECURE_RANDOM = new SecureRandom();

  private final StringRedisTemplate stringRedisTemplate;

  private final AuthProperties authProperties;

  /**
   * 签发会话。同一设备已有会话时先作废旧会话，保证一台设备只有一个有效 token。
   *
   * @param userId   用户ID
   * @param deviceId 设备标识
   * @return 新 token
   */
  public String issue(Long userId, String deviceId) {
    String token = generateToken();
    Duration ttl = tokenTtl();

    // 同一设备重复登录：旧 token 立即失效，避免会话越积越多
    String deviceKey = deviceKey(userId, deviceId);
    String oldToken = stringRedisTemplate.opsForValue().get(deviceKey);
    if (oldToken != null) {
      stringRedisTemplate.delete(sessionKey(oldToken));
    }

    stringRedisTemplate
      .opsForHash()
      .putAll(sessionKey(token), Map.of(FIELD_USER_ID, String.valueOf(userId), FIELD_DEVICE_ID, deviceId));
    stringRedisTemplate.expire(sessionKey(token), ttl);
    stringRedisTemplate.opsForValue().set(deviceKey, token, ttl);
    return token;
  }

  /**
   * 解析 token 并续期。两个 key 都要续：如果只续会话不续索引，
   * 索引先过期后再登录就找不到旧会话，同一设备会留下两个有效 token。
   *
   * @param token 客户端携带的 token
   * @return 登录用户；token 不存在或已过期时返回 null
   */
  public LoginUser resolve(String token) {
    Map<Object, Object> session = stringRedisTemplate.opsForHash().entries(sessionKey(token));
    if (session == null || session.isEmpty()) {
      return null;
    }
    Long userId = Long.valueOf(String.valueOf(session.get(FIELD_USER_ID)));
    String deviceId = String.valueOf(session.get(FIELD_DEVICE_ID));

    Duration ttl = tokenTtl();
    stringRedisTemplate.expire(sessionKey(token), ttl);
    stringRedisTemplate.expire(deviceKey(userId, deviceId), ttl);
    return new LoginUser(userId, deviceId);
  }

  /**
   * 注销该设备当前的会话：设备索引指向哪个 token，就删哪个会话。
   *
   * <p>按设备而非按 token 注销，Controller 无需再解析请求头；
   * 两个 key 的 TTL 始终同步续期，正常情况下不会出现"索引没了但会话还在"。
   *
   * @param userId   用户ID
   * @param deviceId 设备标识
   */
  public void revoke(Long userId, String deviceId) {
    String deviceKey = deviceKey(userId, deviceId);
    String token = stringRedisTemplate.opsForValue().get(deviceKey);
    if (token != null) {
      stringRedisTemplate.delete(sessionKey(token));
    }
    stringRedisTemplate.delete(deviceKey);
  }

  private String generateToken() {
    byte[] bytes = new byte[TOKEN_BYTES];
    SECURE_RANDOM.nextBytes(bytes);
    return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
  }

  private Duration tokenTtl() {
    return Duration.ofSeconds(authProperties.getTokenExpireSeconds());
  }

  private String sessionKey(String token) {
    return RedisKeyConst.AUTH_SESSION_PREFIX + token;
  }

  private String deviceKey(Long userId, String deviceId) {
    return RedisKeyConst.AUTH_USER_DEVICE_PREFIX + userId + ":" + deviceId;
  }

}

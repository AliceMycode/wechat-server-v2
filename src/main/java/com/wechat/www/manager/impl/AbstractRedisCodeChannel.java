package com.wechat.www.manager.impl;

import com.wechat.www.common.constant.RedisKeyConst;
import com.wechat.www.config.SmsProperties;
import com.wechat.www.domain.enums.SmsSceneEnum;
import com.wechat.www.manager.SmsCodeChannel;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.security.SecureRandom;
import java.time.Duration;

/**
 * "服务端生成验证码"类渠道的公共基类：验证码生成、Redis 存储、核验都在这里实现，
 * 子类只负责把已生成的验证码通过具体渠道发出去（模板方法模式）。
 *
 * <p>适用：mock（日志模拟）、dysmsapi（传统短信验证码）。
 * 认证服务（auth）模式验证码由阿里云生成托管，不走本基类，直接实现 SmsCodeChannel。
 */
public abstract class AbstractRedisCodeChannel implements SmsCodeChannel {

  /** 验证码取值上界：nextInt(1_000_000) 产生 0~999999，再格式化补齐前导零成 6 位 */
  private static final int CODE_UPPER_BOUND = 1_000_000;

  protected final StringRedisTemplate stringRedisTemplate;

  protected final SmsProperties smsProperties;

  /** SecureRandom 实例线程安全可复用；验证码是安全敏感数据，不用 Random/ThreadLocalRandom */
  private final SecureRandom secureRandom = new SecureRandom();

  protected AbstractRedisCodeChannel(StringRedisTemplate stringRedisTemplate, SmsProperties smsProperties) {
    this.stringRedisTemplate = stringRedisTemplate;
    this.smsProperties = smsProperties;
  }

  @Override
  public String sendCode(String phone, SmsSceneEnum scene) {
    String code = generateCode();
    // 先发后存：发送失败不会在 Redis 留下"看似有效"的验证码
    String bizId = doSend(phone, scene, code);
    stringRedisTemplate.opsForValue()
        .set(codeKey(scene, phone), code, Duration.ofSeconds(smsProperties.getCodeExpireSeconds()));
    return bizId;
  }

  @Override
  public boolean verifyCode(String phone, SmsSceneEnum scene, String code) {
    String key = codeKey(scene, phone);
    String saved = stringRedisTemplate.opsForValue().get(key);
    if (saved == null || !saved.equals(code)) {
      return false;
    }
    // 核验通过立即作废，防重放；防暴力尝试（错误次数限制）由调用方在业务层实现
    stringRedisTemplate.delete(key);
    return true;
  }

  /**
   * 渠道真实发送动作，由子类实现。
   *
   * @param code 已生成的验证码明文
   * @return 渠道回执 ID
   */
  protected abstract String doSend(String phone, SmsSceneEnum scene, String code);

  private String generateCode() {
    return String.format("%06d", secureRandom.nextInt(CODE_UPPER_BOUND));
  }

  /** 验证码在 Redis 的 key：wechat:sms:code:{scene}:{phone}，与 V1 表设计文档一致 */
  protected String codeKey(SmsSceneEnum scene, String phone) {
    return RedisKeyConst.SMS_CODE_PREFIX + scene.getCode() + ":" + phone;
  }

}

package com.wechat.www.manager.impl;

import com.wechat.www.config.SmsProperties;
import com.wechat.www.domain.enums.SmsSceneEnum;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * 短信渠道 Mock 实现：验证码打日志，Redis 存取走基类，用于本地跑通全链路。
 *
 * <p>sms.mode=mock（或未配置）时生效，与另外两个渠道实现互斥。
 */
@Slf4j
@Component
@ConditionalOnProperty(name = "sms.mode", havingValue = "mock", matchIfMissing = true)
public class MockSmsCodeChannelImpl extends AbstractRedisCodeChannel {

  /** Mock 回执前缀，便于在 sms_record 里一眼认出模拟数据 */
  private static final String MOCK_BIZ_ID_PREFIX = "mock-";

  public MockSmsCodeChannelImpl(StringRedisTemplate stringRedisTemplate, SmsProperties smsProperties) {
    super(stringRedisTemplate, smsProperties);
  }

  @Override
  protected String doSend(String phone, SmsSceneEnum scene, String code) {
    // Mock 模式仅本地使用，验证码明文打日志方便联调（真实渠道实现里严禁打验证码明文）
    log.info("[短信Mock] phone={} scene={} 验证码={}", phone, scene.getCode(), code);
    return MOCK_BIZ_ID_PREFIX + UUID.randomUUID();
  }

}

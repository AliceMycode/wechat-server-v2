package com.wechat.www.common.constant;

/**
 * Redis key 常量。key 必须收敛在此统一维护，禁止业务代码里手写字符串拼接
 * （规约【强制】：不允许魔法值直接出现在代码中）。
 */
public final class RedisKeyConst {

  /**
   * 短信验证码：wechat:sms:code:{scene}:{phone}，value=验证码，TTL=有效期
   */
  public static final String SMS_CODE_PREFIX = "wechat:sms:code:";

  /**
   * 短信发送冷却：wechat:sms:limit:{scene}:{phone}，value=1，TTL=冷却秒数。
   * 按场景隔离：注册发码后切登录可立即再发（阿里云不同方案互不频控，实测验证）
   */
  public static final String SMS_LIMIT_PREFIX = "wechat:sms:limit:";

  /**
   * 短信核验失败计数：wechat:sms:verify:fail:{scene}:{phone}，value=失败次数，TTL=验证码有效期
   */
  public static final String SMS_VERIFY_FAIL_PREFIX = "wechat:sms:verify:fail:";

  private RedisKeyConst() {
  }

}

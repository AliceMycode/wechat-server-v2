package com.wechat.www.common.constant;

/**
 * IP 维度限流阈值。
 *
 * <p>注解参数必须是编译期常量，因此这些值放常量类而不是 application.yml。
 * 限流阈值极少调整，为"可配置"引入 {@code @ConfigurationProperties} 是空转。
 */
public final class RateLimitConst {

  /**
   * 登录：同一 IP 每分钟请求上限。计数不区分成功失败，所以留足余量；
   * 撞库脚本一秒能打几十次，20 次/分足够拦住
   */
  public static final int LOGIN_IP_MINUTE = 20;

  /**
   * 登录：同一 IP 每 24 小时请求上限。NAT 出口百人规模一天几百次登录是常态
   */
  public static final int LOGIN_IP_DAILY = 1000;

  /**
   * 注册：同一 IP 每分钟请求上限。比发码（10 次/分）再紧一档，挡"先囤码再并发注册"
   */
  public static final int REGISTER_IP_MINUTE = 5;

  /**
   * 注册：同一 IP 每 24 小时请求上限
   */
  public static final int REGISTER_IP_DAILY = 50;

  /**
   * 短信发码：同一 IP 每分钟请求上限。手机号维度的冷却与每日上限换号即可绕过，
   * IP 是换号刷短信的唯一闸门
   */
  public static final int SMS_IP_MINUTE = 10;

  /**
   * 短信发码：同一 IP 每 24 小时请求上限
   */
  public static final int SMS_IP_DAILY = 50;

  private RateLimitConst() {
  }

}

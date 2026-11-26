package com.wechat.www.common.constant;

/**
 * 登录鉴权常量。
 *
 * <p>这些是产品策略值，三套 application-*.yml 从未覆盖过，
 * 不随部署环境变化，因此用常量而不是配置项 —— 避免为了"可配置"而引入
 * 一个实际永远只有一套取值的 {@code @ConfigurationProperties}。
 */
public final class AuthConst {

  /**
   * 会话有效期（秒），7 天。每次鉴权通过都会续期，
   * 因此活跃用户不会被强制下线，长期不活跃的会话自然过期
   */
  public static final int TOKEN_EXPIRE_SECONDS = 604800;

  /**
   * 密码连续错误多少次后锁定
   */
  public static final int PASSWORD_MAX_FAIL_COUNT = 5;

  /**
   * 密码错误计数与锁定时长（秒），5 分钟
   */
  public static final int PASSWORD_LOCK_SECONDS = 300;

  private AuthConst() {
  }

}

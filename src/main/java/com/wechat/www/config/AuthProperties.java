package com.wechat.www.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 登录鉴权配置项（application.yml 的 auth.* 前缀）。
 */
@Data
@Component
@ConfigurationProperties(prefix = "auth")
public class AuthProperties {

  /**
   * 会话有效期（秒），默认 7 天。每次鉴权通过都会续期，
   * 因此活跃用户不会被强制下线，长期不活跃的会话自然过期
   */
  private int tokenExpireSeconds = 604800;

  /**
   * 密码连续错误多少次后锁定
   */
  private int passwordMaxFailCount = 5;

  /**
   * 密码错误计数与锁定时长（秒），默认 15 分钟
   */
  private int passwordLockSeconds = 900;

}

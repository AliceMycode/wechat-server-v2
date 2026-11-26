package com.wechat.www.domain.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;

/**
 * 登录结果：注册即登录、密码登录、验证码登录三种入口共用。
 *
 * <p>只返回 token。用户资料统一由 {@code GET /api/user/me} 提供 ——
 * deviceId 是客户端自己传的，会话有效期属于服务端内部策略，都不回传。
 */
@Data
@AllArgsConstructor
public class LoginVO {

  /**
   * 会话 token，后续请求放在 Authorization: Bearer <token>
   */
  @Schema(description = "会话 token，后续请求放 Authorization: Bearer <token>", example =
    "3xQ9mK2pR7tYbV1nLcW8sZfHjD4gA6eUoNi5qJh")
  private String token;

}

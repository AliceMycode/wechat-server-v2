package com.wechat.www.domain.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;

/**
 * 登录结果：注册即登录、密码登录、验证码登录三种入口共用。
 */
@Data
@AllArgsConstructor
public class LoginVO {

  /** 会话 token，后续请求放在 Authorization: Bearer <token> */
  @Schema(description = "会话 token，后续请求放 Authorization: Bearer <token>",
      example = "3xQ9mK2pR7tYbV1nLcW8sZfHjD4gA6eUoNi5qJh")
  private String token;

  /** 用户ID（雪花ID，JSON 中自动输出字符串） */
  @Schema(description = "用户ID（雪花ID，JSON 中以字符串返回避免精度丢失）", example = "726400000000000001")
  private Long userId;

  /** 昵称 */
  @Schema(description = "昵称", example = "微信用户001")
  private String nickname;

  /** 当前登录的设备标识 */
  @Schema(description = "当前登录的设备标识", example = "550e8400-e29b-41d4-a716-446655440000")
  private String deviceId;

  /** 会话有效期（秒），客户端可据此决定何时刷新登录态 */
  @Schema(description = "会话有效期（秒）", example = "604800")
  private Integer expireSeconds;

}

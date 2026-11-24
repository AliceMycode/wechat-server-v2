package com.wechat.www.domain.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;

/**
 * 用户注册结果（Web 层返回给客户端的展示对象）。
 */
@Data
@AllArgsConstructor
public class RegisterUserVO {

  /**
   * 用户ID（雪花ID，超出 JS 安全整数范围，JSON 中自动输出字符串）
   */
  @Schema(description = "用户ID（雪花ID，JSON 中以字符串返回避免精度丢失）", example = "726400000000000001")
  private Long userId;

  /**
   * 昵称
   */
  @Schema(description = "昵称", example = "微信用户001")
  private String nickname;

}

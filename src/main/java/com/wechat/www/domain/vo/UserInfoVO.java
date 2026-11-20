package com.wechat.www.domain.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;

/**
 * 当前登录用户的基本信息。
 */
@Data
@AllArgsConstructor
public class UserInfoVO {

  /** 用户ID（雪花ID，JSON 中自动输出字符串） */
  @Schema(description = "用户ID（雪花ID，JSON 中以字符串返回避免精度丢失）", example = "726400000000000001")
  private Long userId;

  /** 昵称 */
  @Schema(description = "昵称", example = "微信用户001")
  private String nickname;

  /** 手机号（脱敏） */
  @Schema(description = "手机号（中间四位脱敏）", example = "138****8000")
  private String phone;

  /** 当前登录的设备标识 */
  @Schema(description = "当前登录的设备标识", example = "550e8400-e29b-41d4-a716-446655440000")
  private String deviceId;

}

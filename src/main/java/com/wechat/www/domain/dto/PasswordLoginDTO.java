package com.wechat.www.domain.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 手机号 + 密码登录入参。
 *
 * <p>密码这里只做非空校验，不套注册时的强度规则：
 * 历史密码可能不符合后来收紧的策略，登录侧不该因此拒绝。
 */
@Data
public class PasswordLoginDTO {

  /** 国家区号：当前仅支持中国大陆 86，不传时由服务端补默认值 */
  @Schema(description = "国家区号，当前仅支持 86（不传默认 86）", example = "86")
  @Pattern(regexp = "^86$", message = "当前仅支持中国大陆手机号")
  private String countryCode;

  /** 手机号 */
  @Schema(description = "手机号", example = "13800138000")
  @NotBlank(message = "手机号不能为空")
  @Pattern(regexp = "^1[3-9]\\d{9}$", message = "手机号格式不正确")
  private String phone;

  /** 密码 */
  @Schema(description = "密码", example = "Wechat2026")
  @NotBlank(message = "密码不能为空")
  private String password;

  /** 设备标识：客户端首次启动生成并本地持久化的 UUID */
  @Schema(description = "设备标识（客户端生成并本地持久化的 UUID）",
      example = "550e8400-e29b-41d4-a716-446655440000")
  @NotBlank(message = "设备标识不能为空")
  @Size(max = 64, message = "设备标识长度不能超过 64")
  private String deviceId;

  /** 设备类型 1Electron 2iOS 3Android 4Web；不传按 0（未识别）落库 */
  @Schema(description = "设备类型 1Electron 2iOS 3Android 4Web", example = "1")
  private Integer deviceType;

  /** 设备名称 */
  @Schema(description = "设备名称", example = "MacBook Pro")
  @Size(max = 64, message = "设备名称长度不能超过 64")
  private String deviceName;

}

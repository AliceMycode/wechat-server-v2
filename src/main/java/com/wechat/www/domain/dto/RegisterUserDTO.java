package com.wechat.www.domain.dto;

import com.wechat.www.common.constant.SmsConst;
import com.wechat.www.common.constant.UserConst;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 用户注册入参（对标微信注册：昵称 + 国家区号 + 手机号 + 密码，再验短信验证码）。
 *
 * <p>客户端 UI 可以分两步收集，后端一次提交：第一步的信息在本地暂存，
 * 第二步点"完成"时连同验证码一起发送，避免服务端维护注册草稿状态。
 */
@Data
public class RegisterUserDTO {

  /**
   * 国家区号：当前仅支持中国大陆 86，不传时由服务端补默认值
   */
  @Schema(description = "国家区号，当前仅支持 86（不传默认 86）", example = "86")
  @Pattern(regexp = UserConst.COUNTRY_CODE_REGEXP, message = "当前仅支持中国大陆手机号")
  private String countryCode;

  /**
   * 手机号（大陆号段）
   */
  @Schema(description = "手机号", example = "13800138000")
  @NotBlank(message = "手机号不能为空")
  @Pattern(regexp = UserConst.PHONE_REGEXP, message = "手机号格式不正确")
  private String phone;

  /**
   * 密码：8~16 位，字母/数字/符号至少两种，只允许 ASCII 可打印字符
   */
  @Schema(description = "密码：8~16 位，字母/数字/符号至少两种", example = "Wechat2026")
  @NotBlank(message = "密码不能为空")
  private String password;

  /**
   * 昵称
   */
  @Schema(description = "昵称", example = "微信用户001")
  @NotBlank(message = "昵称不能为空")
  @Size(max = UserConst.NICKNAME_MAX_LENGTH, message = "昵称长度不能超过 " + UserConst.NICKNAME_MAX_LENGTH)
  private String nickname;

  /**
   * 短信验证码（必须以 scene=register 发送的那条）
   */
  @Schema(description = "短信验证码", example = "123456")
  @NotBlank(message = "验证码不能为空")
  @Size(min = SmsConst.CODE_MIN_LENGTH, max = SmsConst.CODE_MAX_LENGTH, message = "验证码长度不正确")
  private String code;

  /**
   * 设备标识：客户端首次启动生成并本地持久化的 UUID，注册时落 user_device
   */
  @Schema(description = "设备标识（客户端生成并本地持久化的 UUID）", example = "550e8400-e29b-41d4-a716-446655440000")
  @NotBlank(message = "设备标识不能为空")
  @Size(max = UserConst.DEVICE_ID_MAX_LENGTH, message = "设备标识长度不能超过 " + UserConst.DEVICE_ID_MAX_LENGTH)
  private String deviceId;

  /**
   * 设备类型 1-Electron 2-iOS 3-Android 4-Web；不传按 0（未识别）落库
   */
  @Schema(description = "设备类型 1-Electron 2-iOS 3-Android 4-Web", example = "1")
  @Min(value = UserConst.DEVICE_TYPE_MIN, message = "设备类型取值不正确")
  @Max(value = UserConst.DEVICE_TYPE_MAX, message = "设备类型取值不正确")
  private Integer deviceType;

  /**
   * 设备名称
   */
  @Schema(description = "设备名称", example = "MacBook Pro")
  @Size(max = UserConst.DEVICE_NAME_MAX_LENGTH, message = "设备名称长度不能超过 " + UserConst.DEVICE_NAME_MAX_LENGTH)
  private String deviceName;

}

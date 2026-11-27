package com.wechat.www.domain.dto;

import com.wechat.www.common.constant.DeviceConst;
import com.wechat.www.common.constant.PhoneConst;
import com.wechat.www.common.constant.SmsConst;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 手机号 + 短信验证码登录入参。
 *
 * <p>验证码必须是 scene=login 发出的那一条，注册场景的验证码不能用于登录。
 */
@Data
public class SmsLoginDTO {

  /**
   * 国家区号：当前仅支持中国大陆 86，不传时由服务端补默认值
   */
  @Schema(description = "国家区号，当前仅支持 86（不传默认 86）", example = "86")
  @Pattern(regexp = PhoneConst.COUNTRY_CODE_REGEXP, message = "当前仅支持中国大陆手机号")
  private String countryCode;

  /**
   * 手机号
   */
  @Schema(description = "手机号", example = "13800138000")
  @NotBlank(message = "手机号不能为空")
  @Pattern(regexp = PhoneConst.PHONE_REGEXP, message = "手机号格式不正确")
  private String phone;

  /**
   * 短信验证码
   */
  @Schema(description = "短信验证码（scene=login）", example = "123456")
  @NotBlank(message = "验证码不能为空")
  @Size(min = SmsConst.CODE_MIN_LENGTH, max = SmsConst.CODE_MAX_LENGTH, message = "验证码长度不正确")
  private String code;

  /**
   * 设备标识：客户端首次启动生成并本地持久化的 UUID
   */
  @Schema(description = "设备标识（客户端生成并本地持久化的 UUID）", example = "550e8400-e29b-41d4-a716-446655440000")
  @NotBlank(message = "设备标识不能为空")
  @Size(max = DeviceConst.DEVICE_ID_MAX_LENGTH, message = "设备标识长度不能超过 " + DeviceConst.DEVICE_ID_MAX_LENGTH)
  private String deviceId;

  /**
   * 设备类型 1-Electron 2-iOS 3-Android 4-Web；不传按 0（未识别）落库
   */
  @Schema(description = "设备类型 1-Electron 2-iOS 3-Android 4-Web", example = "1")
  @Min(value = DeviceConst.DEVICE_TYPE_MIN, message = "设备类型取值不正确")
  @Max(value = DeviceConst.DEVICE_TYPE_MAX, message = "设备类型取值不正确")
  private Integer deviceType;

  /**
   * 设备名称
   */
  @Schema(description = "设备名称", example = "MacBook Pro")
  @Size(max = DeviceConst.DEVICE_NAME_MAX_LENGTH, message = "设备名称长度不能超过 " + DeviceConst.DEVICE_NAME_MAX_LENGTH)
  private String deviceName;

}
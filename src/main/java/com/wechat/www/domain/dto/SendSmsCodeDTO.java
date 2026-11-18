package com.wechat.www.domain.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 发送短信验证码入参。
 *
 * <p>@Schema 的 example 会进 OpenAPI 文档，Knife4j 调试面板据此自动填充请求体，
 * 省去每次手写 JSON。
 */
@Data
public class SendSmsCodeDTO {

  /** 手机号（大陆号段） */
  @Schema(description = "手机号", example = "13800138000")
  @NotBlank(message = "手机号不能为空")
  @Pattern(regexp = "^1[3-9]\\d{9}$", message = "手机号格式不正确")
  private String phone;

  /** 场景 register/login/reset */
  @Schema(description = "业务场景", example = "register", allowableValues = {"register", "login", "reset"})
  @NotBlank(message = "场景不能为空")
  private String scene;

  /** 设备标识：客户端首次启动生成并本地持久化的 UUID，落库 sms_record.device_id 用于风控审计 */
  @Schema(description = "设备标识（客户端生成并本地持久化的 UUID，用于风控审计）",
      example = "550e8400-e29b-41d4-a716-446655440000")
  @NotBlank(message = "设备标识不能为空")
  @Size(max = 64, message = "设备标识长度不能超过 64")
  private String deviceId;

}

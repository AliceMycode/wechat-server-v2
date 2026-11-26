package com.wechat.www.domain.dto;

import com.wechat.www.common.constant.SmsConst;
import com.wechat.www.common.constant.UserConst;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 核验短信验证码入参。
 *
 * <p>@Schema 的 example 会进 OpenAPI 文档，Knife4j 调试面板据此自动填充请求体，
 * 省去每次手写 JSON。
 */
@Data
public class VerifySmsCodeDTO {

  /**
   * 手机号（大陆号段）
   */
  @Schema(description = "手机号", example = "13800138000")
  @NotBlank(message = "手机号不能为空")
  @Pattern(regexp = UserConst.PHONE_REGEXP, message = "手机号格式不正确")
  private String phone;

  /**
   * 场景 register/login/reset，必须与发送时一致
   */
  @Schema(description = "业务场景，需与发送时一致", example = "register", allowableValues = {"register", "login", "reset"})
  @NotBlank(message = "场景不能为空")
  private String scene;

  /**
   * 用户收到的验证码：auth 模式长度 4～8 位可配置，dysmsapi 固定 6 位数字
   */
  @Schema(description = "验证码", example = "123456")
  @NotBlank(message = "验证码不能为空")
  @Size(min = SmsConst.CODE_MIN_LENGTH, max = SmsConst.CODE_MAX_LENGTH, message = "验证码长度不正确")
  private String code;

}
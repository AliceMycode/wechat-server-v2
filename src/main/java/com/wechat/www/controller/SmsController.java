package com.wechat.www.controller;

import com.wechat.www.common.annotation.IpRateLimit;
import com.wechat.www.common.constant.RateLimitConst;
import com.wechat.www.common.result.Result;
import com.wechat.www.common.util.IpUtils;
import com.wechat.www.domain.dto.SendSmsCodeDTO;
import com.wechat.www.domain.dto.VerifySmsCodeDTO;
import com.wechat.www.service.SmsService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 短信接口。
 */
@Tag(name = "短信")
@RestController
@RequestMapping("/api/sms")
@RequiredArgsConstructor
public class SmsController {

  private final SmsService smsService;

  /**
   * 发送短信验证码。IP 维度限流在进入方法前由拦截器完成，方法内只做手机号维度频控。
   */
  @IpRateLimit(minute = RateLimitConst.SMS_IP_MINUTE, daily = RateLimitConst.SMS_IP_DAILY)
  @Operation(summary = "发送短信验证码")
  @PostMapping("/code")
  public Result<Void> sendCode(@Valid @RequestBody SendSmsCodeDTO dto, HttpServletRequest request) {
    smsService.sendCode(dto, IpUtils.resolveClientIp(request));
    return Result.ok();
  }

  /**
   * 核验短信验证码。
   */
  @Operation(summary = "核验短信验证码")
  @PostMapping("/verify")
  public Result<Void> verifyCode(@Valid @RequestBody VerifySmsCodeDTO dto) {
    smsService.verifyCode(dto);
    return Result.ok();
  }

}

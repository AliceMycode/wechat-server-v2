package com.wechat.www.controller;

import com.wechat.www.common.result.Result;
import com.wechat.www.domain.dto.SendSmsCodeDTO;
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
   * 发送短信验证码。
   */
  @Operation(summary = "发送短信验证码")
  @PostMapping("/code")
  public Result<Void> sendCode(@Valid @RequestBody SendSmsCodeDTO dto, HttpServletRequest request) {
    smsService.sendCode(dto, getClientIp(request));
    return Result.ok();
  }

  /**
   * 取客户端真实 IP：优先 X-Forwarded-For 首段（经过 Nginx 等代理时由代理写入，
   * 多级代理格式为 "client, proxy1, proxy2"）；未经过代理则取直连地址。
   */
  private String getClientIp(HttpServletRequest request) {
    String xff = request.getHeader("X-Forwarded-For");
    if (xff != null && !xff.isEmpty()) {
      return xff.split(",")[0].trim();
    }
    return request.getRemoteAddr();
  }

}

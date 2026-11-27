package com.wechat.www.controller;

import com.wechat.www.common.annotation.IpRateLimit;
import com.wechat.www.common.constant.RateLimitConst;
import com.wechat.www.common.result.Result;
import com.wechat.www.common.security.LoginUser;
import com.wechat.www.domain.dto.PasswordLoginDTO;
import com.wechat.www.domain.dto.SmsLoginDTO;
import com.wechat.www.domain.vo.LoginVO;
import com.wechat.www.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 登录鉴权接口。
 */
@Tag(name = "认证")
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

  private final AuthService authService;

  /**
   * 手机号 + 密码登录。
   */
  @IpRateLimit(minute = RateLimitConst.LOGIN_IP_MINUTE, daily = RateLimitConst.LOGIN_IP_DAILY)
  @Operation(summary = "手机号 + 密码登录")
  @PostMapping("/login/password")
  public Result<LoginVO> loginByPassword(@Valid @RequestBody PasswordLoginDTO dto) {
    return Result.ok(authService.loginByPassword(dto));
  }

  /**
   * 手机号 + 短信验证码登录（验证码需以 scene=login 发送）。
   */
  @Operation(summary = "手机号 + 短信验证码登录")
  @PostMapping("/login/sms")
  public Result<LoginVO> loginBySms(@Valid @RequestBody SmsLoginDTO dto) {
    return Result.ok(authService.loginBySms(dto));
  }

  /**
   * 登出当前设备：token 立即失效，其他设备不受影响。
   */
  @Operation(summary = "登出当前设备")
  @PostMapping("/logout")
  public Result<Void> logout(@AuthenticationPrincipal LoginUser loginUser) {
    authService.logout(loginUser);
    return Result.ok();
  }

}

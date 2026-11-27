package com.wechat.www.controller;

import com.wechat.www.common.annotation.IpRateLimit;
import com.wechat.www.common.constant.RateLimitConst;
import com.wechat.www.common.result.Result;
import com.wechat.www.common.security.LoginUser;
import com.wechat.www.domain.dto.RegisterUserDTO;
import com.wechat.www.domain.vo.LoginVO;
import com.wechat.www.domain.vo.UserProfileVO;
import com.wechat.www.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 用户接口。
 */
@Tag(name = "用户")
@RestController
@RequestMapping("/api/user")
@RequiredArgsConstructor
public class UserController {

  private final AuthService authService;

  /**
   * 用户注册：注册成功即登录，直接返回 token，前端无需再跳登录页。
   */
  @IpRateLimit(maxPerMinute = RateLimitConst.REGISTER_IP_MINUTE, maxPerDay = RateLimitConst.REGISTER_IP_DAILY)
  @Operation(summary = "用户注册（注册即登录）")
  @PostMapping("/register")
  public Result<LoginVO> register(@Valid @RequestBody RegisterUserDTO dto) {
    return Result.ok(authService.registerAndLogin(dto));
  }

  /**
   * 当前登录用户信息。
   */
  @Operation(summary = "当前登录用户信息")
  @GetMapping("/me")
  public Result<UserProfileVO> me(@AuthenticationPrincipal LoginUser loginUser) {
    return Result.ok(authService.currentUser(loginUser));
  }

}
package com.wechat.www.controller;

import com.wechat.www.common.result.Result;
import com.wechat.www.domain.dto.RegisterUserDTO;
import com.wechat.www.domain.vo.RegisterUserVO;
import com.wechat.www.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
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

  private final UserService userService;

  /**
   * 用户注册。
   */
  @Operation(summary = "用户注册")
  @PostMapping("/register")
  public Result<RegisterUserVO> register(@Valid @RequestBody RegisterUserDTO dto) {
    return Result.ok(userService.register(dto));
  }

}

package com.wechat.www.service;

import com.wechat.www.common.security.LoginUser;
import com.wechat.www.domain.dto.PasswordLoginDTO;
import com.wechat.www.domain.dto.RegisterUserDTO;
import com.wechat.www.domain.dto.SmsLoginDTO;
import com.wechat.www.domain.vo.LoginVO;
import com.wechat.www.domain.vo.UserProfileVO;

/**
 * 登录鉴权服务。
 */
public interface AuthService {

  /**
   * 注册并登录：先完成注册，事务提交后再签发会话。
   *
   * @param dto 注册入参
   * @return 登录结果（含 token）
   */
  LoginVO registerAndLogin(RegisterUserDTO dto);

  /**
   * 手机号 + 密码登录。账号不存在与密码错误统一返回「手机号或密码错误」。
   *
   * @param dto 登录入参
   * @return 登录结果
   */
  LoginVO loginByPassword(PasswordLoginDTO dto);

  /**
   * 手机号 + 短信验证码登录（scene=login）。
   *
   * @param dto 登录入参
   * @return 登录结果
   */
  LoginVO loginBySms(SmsLoginDTO dto);

  /**
   * 登出当前设备：作废 Redis 会话，该 token 立即失效。
   *
   * @param loginUser 当前登录用户
   */
  void logout(LoginUser loginUser);

  /**
   * 查询当前登录用户的资料。
   *
   * @param loginUser 当前登录用户
   * @return 用户资料（对外 VO，DO 不出 Service 层）
   */
  UserProfileVO currentUser(LoginUser loginUser);

}

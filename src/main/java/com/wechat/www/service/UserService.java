package com.wechat.www.service;

import com.wechat.www.domain.dto.RegisterUserDTO;

/**
 * 用户服务。
 */
public interface UserService {

  /**
   * 用户注册：密码策略校验 → 短信验证码核验（scene=register） → 手机号查重 →
   * 事务内写入 user_account / user_profile / user_device。
   *
   * @param dto 注册入参
   * @return 新用户ID
   */
  Long register(RegisterUserDTO dto);

}

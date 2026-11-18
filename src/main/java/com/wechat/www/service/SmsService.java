package com.wechat.www.service;

import com.wechat.www.domain.dto.SendSmsCodeDTO;
import com.wechat.www.domain.dto.VerifySmsCodeDTO;

/**
 * 短信服务。
 */
public interface SmsService {

  /**
   * 发送短信验证码：频控 → 生成验证码 → 渠道发送 → 落库 → 写入 Redis。
   *
   * @param dto 入参（手机号、场景、设备标识）
   * @param ip  请求来源 IP，落库用于风控审计（sms_record 的 idx_ip_create 索引）
   */
  void sendCode(SendSmsCodeDTO dto, String ip);

  /**
   * 核验短信验证码：失败次数限制 → 渠道核验 → 成功清除失败计数。
   * 不通过抛 BizException（A0240 验证码错误 / A0241 尝试次数超限）。
   *
   * @param dto 入参（手机号、场景、验证码）
   */
  void verifyCode(VerifySmsCodeDTO dto);

}

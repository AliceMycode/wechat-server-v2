package com.wechat.www.service;

import com.wechat.www.domain.dto.SendSmsCodeDTO;

/**
 * 短信服务。
 */
public interface SmsService {

  /**
   * 发送短信验证码：频控 → 生成验证码 → 渠道发送 → 落库 → 写入 Redis。
   *
   * @param dto 入参（手机号、场景）
   * @param ip  请求来源 IP，落库用于风控审计（sms_record 的 idx_ip_create 索引）
   */
  void sendCode(SendSmsCodeDTO dto, String ip);

}

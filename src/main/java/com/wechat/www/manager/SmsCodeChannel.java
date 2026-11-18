package com.wechat.www.manager;

import com.wechat.www.domain.dto.SmsSendResultDTO;
import com.wechat.www.domain.enums.SmsSceneEnum;

/**
 * 短信验证码渠道：封装验证码的"发送 + 核验"完整生命周期。
 *
 * <p>不同渠道模式的差异被隔离在实现类里，Service 层零感知：
 * <ul>
 *   <li>auth：短信认证服务，验证码由阿里云生成、发送、托管核验（无需企业资质）</li>
 *   <li>dysmsapi：传统短信验证码，服务端生成验证码、渠道发送、Redis 核验（预留，需企业资质签名）</li>
 * </ul>
 * 切换模式只改 sms.mode 配置，业务代码零改动（Manager 层屏蔽第三方差异的典型应用）。
 */
public interface SmsCodeChannel {

  /**
   * 发送短信验证码。
   *
   * @param phone 接收手机号
   * @param scene 业务场景，决定使用渠道的哪个方案/模板
   * @return 渠道回执（bizId + requestId），用于 sms_record 落库与对账；失败抛 BizException
   */
  SmsSendResultDTO sendCode(String phone, SmsSceneEnum scene);

  /**
   * 核验用户输入的验证码。
   *
   * @return true 核验通过（实现类自行作废该验证码，防重放）；false 不通过（过期/错误）
   */
  boolean verifyCode(String phone, SmsSceneEnum scene, String code);

}

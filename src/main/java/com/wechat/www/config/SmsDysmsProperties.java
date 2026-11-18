package com.wechat.www.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 传统短信验证码（dysmsapi 模式）专属配置项（application.yml 的 sms.dysms.* 前缀）。
 *
 * <p>签名与模板取"传统短信"体系：自定义签名（需企业资质）+ 审核模板（SMS_xxx 格式），
 * 与认证服务（sms.auth.*）的取值体系不同，故拆分独立配置。
 */
@Data
@Component
@ConfigurationProperties(prefix = "sms.dysms")
public class SmsDysmsProperties {

  /**
   * 短信签名名称：dysmsapi 模式必填，自定义签名（需企业资质审核）
   */
  private String signName;

  /**
   * 短信模板 CODE：dysmsapi 模式必填，审核模板 SMS_xxx 格式
   */
  private String templateCode;

}

package com.wechat.www.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

/**
 * 短信认证服务（auth 模式）专属配置项（application.yml 的 sms.auth.* 前缀）。
 *
 * <p>签名与模板取"短信认证服务"体系：赠送签名（如"北京恒创联众"）+ 赠送模板（100001 格式），
 * 与传统短信（sms.dysms.*）的取值体系不同，故拆分独立配置。
 */
@Data
@Component
@ConfigurationProperties(prefix = "sms.auth")
public class SmsAuthProperties {

  /**
   * 方案名（SendSmsVerifyCode 的 SchemeName），按业务场景配置，key 为场景标识（register/login/reset）。
   *
   * <p>阿里云按方案隔离验证码：不同场景用不同方案名，验证码互不覆盖（实测验证：
   * 方案 A 的码在方案 B 核验返回 400，反之亦然）。方案名任意字符串即可，无需控制台预先创建。
   */
  private Map<String, String> schemeName = new HashMap<>();

  /**
   * 号码国家编码，发送/核验两侧必须一致。阿里云目前仅支持国内发送，默认 86
   */
  private String countryCode = "86";

  /**
   * 生成的验证码类型：1-纯数字 2-纯大写字母 3-纯小写字母 4-大小写字母混合
   * 5-数字+大写字母 6-数字+小写字母 7-数字+大小写字母（TemplateParam 传占位符时必填）
   */
  private Long codeType = 1L;

  /**
   * 验证码长度，4～8 位（默认 4，本项目显式配 6）
   */
  private Long codeLength = 6L;

  /**
   * 短信签名名称：必填（SendSmsVerifyCode 实测 400 强校验），短信套餐包赠送的签名
   */
  private String signName;

  /**
   * 短信模板 CODE：必填（SendSmsVerifyCode 实测 400 强校验），赠送模板 100001 格式
   */
  private String templateCode;

}

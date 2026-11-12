package com.wechat.www.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 短信配置项（application.yml 的 sms.* 前缀）。
 */
@Data
@Component
@ConfigurationProperties(prefix = "sms")
public class SmsProperties {

  /**
   * 渠道模式：mock=本地日志（开发联调） / auth=短信认证服务（当前使用，无需企业资质）
   * / dysmsapi=传统短信验证码（预留，需企业资质签名）
   */
  private String mode = "mock";

  /** 阿里云 AccessKey ID（环境变量 ALIYUN_SMS_ACCESS_KEY_ID 注入，禁止写死提交 git） */
  private String accessKeyId;

  /** 阿里云 AccessKey Secret */
  private String accessKeySecret;

  /** auth 模式：短信认证服务控制台创建的方案名（SchemeName） */
  private String authSchemeName;

  /** dysmsapi 模式（预留）：短信签名 */
  private String signName;

  /** dysmsapi 模式（预留）：验证码模板 CODE */
  private String templateCode;

  /** 同一手机号两次发送的最小间隔（秒） */
  private int sendIntervalSeconds = 60;

  /** 同一手机号每天最多发送条数 */
  private int dailyLimit = 10;

  /** 验证码有效期（秒）：仅 mock/dysmsapi 模式生效，auth 模式有效期在控制台方案里配置 */
  private int codeExpireSeconds = 300;

}

package com.wechat.www.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 短信公共配置项（application.yml 的 sms.* 前缀）。
 *
 * <p>只放各渠道模式共享的配置；模式专属配置拆分在
 * {@link SmsAuthProperties}（sms.auth.*）和 {@link SmsDysmsProperties}（sms.dysms.*），
 * 因为认证服务与传统短信的签名/模板取值体系不同（赠送签名+100001 模板 vs 自定义签名+SMS_xxx 模板）。
 */
@Data
@Component
@ConfigurationProperties(prefix = "sms")
public class SmsProperties {

  /**
   * 渠道模式：auth=短信认证服务（当前使用，无需企业资质）
   * / dysmsapi=传统短信验证码（预留，需企业资质签名）
   */
  private String mode = "auth";

  /**
   * 阿里云 AccessKey ID（环境变量 ALIYUN_SMS_ACCESS_KEY_ID 注入，禁止写死提交 git）
   */
  private String accessKeyId;

  /**
   * 阿里云 AccessKey Secret
   */
  private String accessKeySecret;

  /**
   * 同一手机号两次发送的最小间隔（秒）
   */
  private int sendIntervalSeconds = 60;

  /**
   * 同一手机号每天最多发送条数
   */
  private int dailyLimit = 10;

  /**
   * 同一 IP 每分钟最多发送次数
   */
  private int ipMinuteLimit = 10;

  /**
   * 同一 IP 每 24 小时最多发送次数
   */
  private int ipDailyLimit = 50;

  /**
   * 验证码有效期（秒）：auth 模式作为 ValidTime 传给阿里云；dysmsapi 模式用于本地 Redis TTL
   */
  private int codeExpireSeconds = 300;

}

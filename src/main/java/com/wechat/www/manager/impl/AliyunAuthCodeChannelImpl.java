package com.wechat.www.manager.impl;

import com.aliyun.dypnsapi20170525.Client;
import com.aliyun.dypnsapi20170525.models.CheckSmsVerifyCodeRequest;
import com.aliyun.dypnsapi20170525.models.CheckSmsVerifyCodeResponseBody;
import com.aliyun.dypnsapi20170525.models.SendSmsVerifyCodeRequest;
import com.aliyun.dypnsapi20170525.models.SendSmsVerifyCodeResponseBody;
import com.aliyun.teaopenapi.models.Config;
import com.wechat.www.common.exception.BizException;
import com.wechat.www.common.exception.ErrorCode;
import com.wechat.www.common.util.PhoneUtils;
import com.wechat.www.config.SmsProperties;
import com.wechat.www.domain.enums.SmsSceneEnum;
import com.wechat.www.manager.SmsCodeChannel;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * 短信渠道阿里云"短信认证服务"实现（Dypnsapi SDK，sms.mode=auth 时生效）。
 *
 * <p>与 dysmsapi 模式的本质区别：验证码由阿里云生成、发送、托管核验，
 * 本服务不生成验证码、不存 Redis，verifyCode 直接调渠道核验接口。
 * 不需要企业资质，签名可走短信套餐包赠送的自定义签名。
 *
 * <p>前提：短信认证服务控制台 → 方案管理 → 创建"短信验证码"类型的方案，
 * 方案名配置到 sms.auth-scheme-name；签名配置到 sms.sign-name（SendSmsVerifyCode 必填）。
 */
@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "sms.mode", havingValue = "auth")
public class AliyunAuthCodeChannelImpl implements SmsCodeChannel {

  /** 阿里云号码认证/短信认证服务端点 */
  private static final String ENDPOINT = "dypnsapi.aliyuncs.com";

  /** 核验通过的结果值（阿里云文档约定，不通过为 REJECT） */
  private static final String VERIFY_RESULT_PASS = "PASS";

  /** 模板参数里验证码的占位符：阿里云动态生成验证码并托管核验（验证码不经过本服务） */
  private static final String CODE_PLACEHOLDER = "##code##";

  private final SmsProperties smsProperties;

  /** SDK 客户端线程安全，应用内复用一个实例即可 */
  private Client client;

  /**
   * 启动时创建客户端，AK 配错直接启动失败（fail fast，不带病运行）。
   */
  @PostConstruct
  public void init() throws Exception {
    if (isBlank(smsProperties.getAccessKeyId()) || isBlank(smsProperties.getAccessKeySecret())) {
      throw new IllegalStateException("sms.mode=auth 但未配置阿里云 AccessKey（ALIYUN_SMS_ACCESS_KEY_ID/SECRET）");
    }
    // SendSmsVerifyCode 接口把 SignName/TemplateCode 都设为必填（实测 400 报错），启动时一并 fail fast
    if (isBlank(smsProperties.getSignName())) {
      throw new IllegalStateException("sms.mode=auth 但未配置签名名称（ALIYUN_SMS_SIGN_NAME）");
    }
    if (isBlank(smsProperties.getTemplateCode())) {
      throw new IllegalStateException("sms.mode=auth 但未配置模板 CODE（ALIYUN_SMS_TEMPLATE_CODE）");
    }
    Config config = new Config()
        .setAccessKeyId(smsProperties.getAccessKeyId())
        .setAccessKeySecret(smsProperties.getAccessKeySecret())
        .setEndpoint(ENDPOINT)
        // 服务器规约【强制】：调用远程操作必须有超时设置
        .setConnectTimeout(3000)
        .setReadTimeout(5000);
    client = new Client(config);
    log.info("阿里云短信认证服务客户端初始化完成 endpoint={} signName={}", ENDPOINT, smsProperties.getSignName());
  }

  @Override
  public String sendCode(String phone, SmsSceneEnum scene) {
    // 验证码由阿里云生成，这里传手机号、方案名、签名、模板；发送间隔/有效期复用 sms 配置
    long validTime = smsProperties.getCodeExpireSeconds();
    // 模板参数：##code## 占位符由阿里云按 CodeType 规则生成并托管核验；min 为有效期分钟数
    String templateParam = "{\"code\":\"" + CODE_PLACEHOLDER + "\",\"min\":\"" + (validTime / 60) + "\"}";
    SendSmsVerifyCodeRequest request = new SendSmsVerifyCodeRequest()
        .setPhoneNumber(phone)
        .setSchemeName(smsProperties.getAuthSchemeName())
        .setSignName(smsProperties.getSignName())
        .setTemplateCode(smsProperties.getTemplateCode())
        .setTemplateParam(templateParam)
        // CodeType 传占位符时必填，1=纯数字；CodeLength 默认 4 位，显式传 6 位
        .setCodeType(1L)
        .setCodeLength(6L)
        .setValidTime(validTime)
        // 渠道侧频控，与应用侧 60 秒冷却保持一致，双保险
        .setInterval((long) smsProperties.getSendIntervalSeconds());
    try {
      SendSmsVerifyCodeResponseBody body = client.sendSmsVerifyCode(request).getBody();
      // 注意：HTTP 200 不代表发送成功，必须校验业务 success 标志
      if (!Boolean.TRUE.equals(body.getSuccess())) {
        // 错误信息带上渠道错误码（isv.* 等）便于排查
        throw new BizException(ErrorCode.SMS_SEND_FAILED, "短信发送失败: " + body.getCode());
      }
      String bizId = body.getModel().getBizId();
      log.info("认证服务短信验证码发送成功 phone={} bizId={}", PhoneUtils.mask(phone), bizId);
      return bizId;
    } catch (BizException e) {
      // 业务异常原样抛出，不能被下面的兜底 catch 再包一层
      throw e;
    } catch (Exception e) {
      // SDK 方法声明 throws Exception（网络超时、鉴权失败等），统一转义为业务异常
      log.error("认证服务短信验证码发送异常 phone={}", PhoneUtils.mask(phone), e);
      throw new BizException(ErrorCode.SMS_SEND_FAILED, "短信发送失败，请稍后再试");
    }
  }

  @Override
  public boolean verifyCode(String phone, SmsSceneEnum scene, String code) {
    CheckSmsVerifyCodeRequest request = new CheckSmsVerifyCodeRequest()
        .setPhoneNumber(phone)
        .setSchemeName(smsProperties.getAuthSchemeName())
        .setVerifyCode(code);
    try {
      CheckSmsVerifyCodeResponseBody body = client.checkSmsVerifyCode(request).getBody();
      if (!Boolean.TRUE.equals(body.getSuccess())) {
        // 验证码过期/错误等渠道业务失败：按"核验不通过"处理，不抛异常打断注册登录流程
        log.warn("认证服务验证码核验未通过 phone={} code={}", PhoneUtils.mask(phone), body.getCode());
        return false;
      }
      return VERIFY_RESULT_PASS.equals(body.getModel().getVerifyResult());
    } catch (Exception e) {
      // 渠道调用异常属于系统错误，不能伪装成"验证码错误"误导用户
      log.error("认证服务验证码核验异常 phone={}", PhoneUtils.mask(phone), e);
      throw new BizException(ErrorCode.SYSTEM_ERROR, "验证码核验失败，请稍后再试");
    }
  }

  private boolean isBlank(String value) {
    return value == null || value.isBlank();
  }

}

package com.wechat.www.manager.impl;

import com.aliyun.dypnsapi20170525.Client;
import com.aliyun.dypnsapi20170525.models.CheckSmsVerifyCodeRequest;
import com.aliyun.dypnsapi20170525.models.CheckSmsVerifyCodeResponseBody;
import com.aliyun.dypnsapi20170525.models.SendSmsVerifyCodeRequest;
import com.aliyun.dypnsapi20170525.models.SendSmsVerifyCodeResponseBody;
import com.aliyun.teaopenapi.models.Config;
import com.wechat.www.common.constant.AliyunSmsConst;
import com.wechat.www.common.constant.SmsConst;
import com.wechat.www.common.exception.BizException;
import com.wechat.www.common.exception.ErrorCodeEnum;
import com.wechat.www.common.util.PhoneUtils;
import com.wechat.www.config.SmsAuthProperties;
import com.wechat.www.config.SmsProperties;
import com.wechat.www.domain.dto.SmsSendResultDTO;
import com.wechat.www.domain.enums.SmsSceneEnum;
import com.wechat.www.manager.SmsCodeChannel;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.HashSet;
import java.util.Set;

/**
 * 短信渠道阿里云"短信认证服务"实现（Dypnsapi SDK，sms.mode=auth 时生效）。
 *
 * <p>与 dysmsapi 模式的本质区别：验证码由阿里云生成、发送、托管核验，
 * 本服务不生成验证码、不存 Redis，verifyCode 直接调渠道核验接口。
 * 不需要企业资质，签名可走短信套餐包赠送的自定义签名。
 *
 * <p>方案名按场景配置到 sms.auth.scheme-name.{scene}：阿里云按方案隔离验证码，
 * 不同场景用不同方案名互不覆盖（实测验证，无需控制台预先创建）；签名配置到 sms.auth.sign-name（必填）。
 */
@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "sms.mode", havingValue = "auth")
public class AliyunAuthCodeChannelImpl implements SmsCodeChannel {

  private final SmsProperties smsProperties;

  /**
   * 认证服务专属配置（方案名/签名/模板）
   */
  private final SmsAuthProperties authProperties;

  /**
   * SDK 客户端线程安全，应用内复用一个实例即可
   */
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
    if (isBlank(authProperties.getSignName())) {
      throw new IllegalStateException("sms.mode=auth 但未配置签名名称（ALIYUN_SMS_AUTH_SIGN_NAME）");
    }
    // 每个场景必须有独立方案名与模板 CODE：方案名用于隔离验证码，模板决定短信文案
    Set<String> usedSchemes = new HashSet<>();
    for (SmsSceneEnum scene : SmsSceneEnum.values()) {
      String scheme = schemeNameOf(scene);
      if (isBlank(scheme)) {
        throw new IllegalStateException("sms.mode=auth 但场景 " + scene.getCode()
          + " 未配置方案名（sms.auth.scheme-name." + scene.getCode() + "）");
      }
      if (isBlank(templateCodeOf(scene))) {
        throw new IllegalStateException("sms.mode=auth 但场景 " + scene.getCode()
          + " 未配置模板 CODE（sms.auth.template-code." + scene.getCode() + "）");
      }
      if (!usedSchemes.add(scheme)) {
        throw new IllegalStateException("sms.mode=auth 但多个场景使用了相同方案名 '" + scheme + "'，验证码会互相覆盖");
      }
    }
    Config config = new Config()
      .setAccessKeyId(smsProperties.getAccessKeyId())
      .setAccessKeySecret(smsProperties.getAccessKeySecret())
      .setEndpoint(AliyunSmsConst.AUTH_ENDPOINT)
      // 服务器规约【强制】：调用远程操作必须有超时设置
      .setConnectTimeout(3000)
      .setReadTimeout(5000);
    client = new Client(config);
    log.info("阿里云短信认证服务客户端初始化完成 endpoint={} signName={} schemes={}",
      AliyunSmsConst.AUTH_ENDPOINT, authProperties.getSignName(), authProperties.getSchemeName());
  }

  @Override
  public SmsSendResultDTO sendCode(String phone, SmsSceneEnum scene) {
    // 验证码由阿里云生成，这里传手机号、方案名、签名、模板；发送间隔/有效期复用 sms 配置
    long validTime = smsProperties.getCodeExpireSeconds();
    // 模板参数：##code## 占位符由阿里云按 CodeType 规则生成并托管核验；min 为有效期分钟数
    String templateParam = "{\"code\":\"" + AliyunSmsConst.AUTH_CODE_PLACEHOLDER + "\",\"min\":\""
      + (validTime / SmsConst.SECONDS_PER_MINUTE) + "\"}";
    SendSmsVerifyCodeRequest request = new SendSmsVerifyCodeRequest()
      .setPhoneNumber(phone)
      .setSchemeName(schemeNameOf(scene))
      .setSignName(authProperties.getSignName())
      .setTemplateCode(templateCodeOf(scene))
      .setTemplateParam(templateParam)
      // 验证码规格从配置取（默认 1=纯数字、6 位），需要换字母混合等类型时改配置不改代码
      .setCodeType(authProperties.getCodeType())
      .setCodeLength(authProperties.getCodeLength())
      .setCountryCode(authProperties.getCountryCode())
      .setValidTime(validTime)
      // 渠道侧频控，与应用侧 60 秒冷却保持一致，双保险
      .setInterval((long) smsProperties.getSendIntervalSeconds());
    try {
      SendSmsVerifyCodeResponseBody body = client.sendSmsVerifyCode(request).getBody();
      // 注意：HTTP 200 不代表发送成功，必须校验业务 success 标志，false/true:短信发送失败/成功
      if (!Boolean.TRUE.equals(body.getSuccess())) {
        // 渠道错误码（isv.* 等）只进日志：BizException 的 message 会原样返回前端，不能带出渠道细节
        log.warn("认证服务短信验证码发送失败 phone={} channelCode={}", PhoneUtils.mask(phone), body.getCode());
        throw new BizException(ErrorCodeEnum.SMS_SEND_FAILED);
      }
      String bizId = body.getModel().getBizId();
      log.info("认证服务短信验证码发送成功 phone={} bizId={} requestId={}", PhoneUtils.mask(phone), bizId,
        body.getRequestId());
      return new SmsSendResultDTO(bizId, body.getRequestId());
    } catch (BizException e) {
      // 业务异常原样抛出，不能被下面的兜底 catch 再包一层
      throw e;
    } catch (Exception e) {
      // SDK 方法声明 throws Exception（网络超时、鉴权失败等），统一转义为业务异常
      log.error("认证服务短信验证码发送异常 phone={}", PhoneUtils.mask(phone), e);
      throw new BizException(ErrorCodeEnum.SYSTEM_ERROR);
    }
  }

  @Override
  public boolean verifyCode(String phone, SmsSceneEnum scene, String code) {
    CheckSmsVerifyCodeRequest request = new CheckSmsVerifyCodeRequest()
      .setPhoneNumber(phone)
      .setSchemeName(schemeNameOf(scene))
      .setCountryCode(authProperties.getCountryCode())
      .setVerifyCode(code);
    try {
      CheckSmsVerifyCodeResponseBody body = client.checkSmsVerifyCode(request).getBody();
      if (!Boolean.TRUE.equals(body.getSuccess())) {
        // 验证码过期/错误等渠道业务失败：按"核验不通过"处理，不抛异常打断注册登录流程
        log.warn("认证服务验证码核验未通过 phone={} code={}", PhoneUtils.mask(phone), body.getCode());
        return false;
      }
      return AliyunSmsConst.AUTH_VERIFY_RESULT_PASS.equals(body.getModel().getVerifyResult());
    } catch (Exception e) {
      // 渠道调用异常属于系统错误，不能伪装成"验证码错误"误导用户
      log.error("认证服务验证码核验异常 phone={}", PhoneUtils.mask(phone), e);
      throw new BizException(ErrorCodeEnum.SYSTEM_ERROR);
    }
  }

  /**
   * 取场景对应的方案名：发送与核验必须使用同一方案，否则核验必失败
   */
  private String schemeNameOf(SmsSceneEnum scene) {
    return authProperties.getSchemeName().get(scene.getCode());
  }

  /**
   * 取场景对应的模板 CODE：不同场景短信文案不同（如登录用 100001、重置密码用 100003）
   */
  private String templateCodeOf(SmsSceneEnum scene) {
    return authProperties.getTemplateCode().get(scene.getCode());
  }

  private boolean isBlank(String value) {
    return value == null || value.isBlank();
  }

}

package com.wechat.www.manager.impl;

import com.aliyun.dysmsapi20170525.Client;
import com.aliyun.dysmsapi20170525.models.SendSmsRequest;
import com.aliyun.dysmsapi20170525.models.SendSmsResponse;
import com.aliyun.teaopenapi.models.Config;
import com.wechat.www.common.constant.AliyunSmsConst;
import com.wechat.www.common.constant.RedisKeyConst;
import com.wechat.www.common.exception.BizException;
import com.wechat.www.common.exception.ErrorCodeEnum;
import com.wechat.www.common.util.PhoneUtils;
import com.wechat.www.config.SmsDysmsProperties;
import com.wechat.www.config.SmsProperties;
import com.wechat.www.domain.dto.SmsSendResultDTO;
import com.wechat.www.domain.enums.SmsSceneEnum;
import com.wechat.www.manager.SmsCodeChannel;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.security.SecureRandom;
import java.time.Duration;

/**
 * 短信渠道阿里云"传统短信验证码"实现（Dysmsapi SDK，sms.mode=dysmsapi 时生效）。
 *
 * <p>当前为预留实现：签名需要企业资质审核，个人主体暂不可用；
 * 将来资质下来后把 sms.mode 改为 dysmsapi 即可切换，业务代码零改动。
 * 验证码由本服务生成并托管在 Redis（生成、发送、核验都在本类），渠道只负责把码发出去。
 */
@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "sms.mode", havingValue = "dysmsapi")
public class AliyunDysmsCodeChannelImpl implements SmsCodeChannel {

  private final RedisTemplate<String, Object> redisTemplate;
  private final SmsProperties smsProperties;
  private final SmsDysmsProperties dysmsProperties;
  /**
   * SecureRandom 实例线程安全可复用；验证码是安全敏感数据，不用 Random/ThreadLocalRandom
   */
  private final SecureRandom secureRandom = new SecureRandom();
  /**
   * SDK 客户端线程安全，应用内复用一个实例即可
   */
  private Client client;

  /**
   * 启动时创建客户端，AK 配错直接启动失败（fail fast，不带病运行）。
   */
  @PostConstruct
  public void init() throws Exception {
    if (smsProperties.getAccessKeyId() == null || smsProperties.getAccessKeyId().isBlank()
      || smsProperties.getAccessKeySecret() == null || smsProperties.getAccessKeySecret().isBlank()) {
      throw new IllegalStateException("sms.mode=dysmsapi 但未配置阿里云 AccessKey（ALIYUN_SMS_ACCESS_KEY_ID/SECRET）");
    }
    Config config = new Config()
      .setAccessKeyId(smsProperties.getAccessKeyId())
      .setAccessKeySecret(smsProperties.getAccessKeySecret())
      .setEndpoint(AliyunSmsConst.DYSMS_ENDPOINT)
      // 服务器规约【强制】：调用远程操作必须有超时设置
      .setConnectTimeout(AliyunSmsConst.CONNECT_TIMEOUT_MILLIS)
      .setReadTimeout(AliyunSmsConst.READ_TIMEOUT_MILLIS);
    client = new Client(config);
    log.info("阿里云短信（Dysmsapi）客户端初始化完成 endpoint={}", AliyunSmsConst.DYSMS_ENDPOINT);
  }

  @Override
  public SmsSendResultDTO sendCode(String phone, SmsSceneEnum scene) {
    String code = generateCode();
    // 先发后存：发送失败不会在 Redis 留下"看似有效"的验证码
    SmsSendResultDTO result = doSend(phone, scene, code);
    redisTemplate.opsForValue().set(codeKey(scene, phone), code,
      Duration.ofSeconds(smsProperties.getCodeExpireSeconds()));
    return result;
  }

  @Override
  public boolean verifyCode(String phone, SmsSceneEnum scene, String code) {
    String key = codeKey(scene, phone);
    Object value = redisTemplate.opsForValue().get(key);
    if (!(value instanceof String saved) || !saved.equals(code)) {
      return false;
    }
    // 核验通过立即作废，防重放；防暴力尝试（错误次数限制）由调用方在业务层实现
    redisTemplate.delete(key);
    return true;
  }

  /**
   * 渠道真实发送动作。
   *
   * @param code 已生成的验证码明文
   * @return 渠道回执（bizId + requestId）
   */
  private SmsSendResultDTO doSend(String phone, SmsSceneEnum scene, String code) {
    // 验证码是纯数字，手工拼 JSON 无注入风险；以后模板参数多了再抽 Map + 序列化
    SendSmsRequest request = new SendSmsRequest().setPhoneNumbers(phone)
      .setSignName(dysmsProperties.getSignName())
      .setTemplateCode(dysmsProperties.getTemplateCode())
      .setTemplateParam("{\"code\":\"" + code + "\"}");
    try {
      SendSmsResponse response = client.sendSms(request);
      String bizCode = response.getBody().getCode();
      // 注意：HTTP 200 不代表发送成功，必须校验业务成功码 "OK"
      if (!AliyunSmsConst.DYSMS_SUCCESS_CODE.equals(bizCode)) {
        // 渠道错误码（isv.BUSINESS_LIMIT_CONTROL 等）只进日志：GlobalExceptionHandler 会把 message 原样返回前端
        log.warn("Dysmsapi 短信发送失败 phone={} channelCode={}", PhoneUtils.mask(phone), bizCode);
        throw new BizException(ErrorCodeEnum.SMS_SEND_FAILED);
      }
      return new SmsSendResultDTO(response.getBody().getBizId(), response.getBody().getRequestId());
    } catch (BizException e) {
      // 业务异常原样抛出，不能被下面的兜底 catch 再包一层
      throw e;
    } catch (Exception e) {
      // SDK 方法声明 throws Exception（网络超时、鉴权失败等），统一转义为业务异常
      log.error("Dysmsapi 短信发送异常 phone={}", PhoneUtils.mask(phone), e);
      throw new BizException(ErrorCodeEnum.SYSTEM_ERROR);
    }
  }

  private String generateCode() {
    return String.format(AliyunSmsConst.DYSMS_CODE_FORMAT, secureRandom.nextInt(AliyunSmsConst.DYSMS_CODE_UPPER_BOUND));
  }

  /**
   * 验证码在 Redis 的 key：wechat:sms:code:{scene}:{phone}，与 V1 表设计文档一致
   */
  private String codeKey(SmsSceneEnum scene, String phone) {
    return RedisKeyConst.SMS_CODE_PREFIX + scene.getCode() + ":" + phone;
  }

}

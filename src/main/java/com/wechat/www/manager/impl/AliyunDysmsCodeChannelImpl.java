package com.wechat.www.manager.impl;

import com.aliyun.dysmsapi20170525.Client;
import com.aliyun.dysmsapi20170525.models.SendSmsRequest;
import com.aliyun.dysmsapi20170525.models.SendSmsResponse;
import com.aliyun.teaopenapi.models.Config;
import com.wechat.www.common.exception.BizException;
import com.wechat.www.common.exception.ErrorCode;
import com.wechat.www.common.util.PhoneUtils;
import com.wechat.www.config.SmsProperties;
import com.wechat.www.domain.enums.SmsSceneEnum;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

/**
 * 短信渠道阿里云"传统短信验证码"实现（Dysmsapi SDK，sms.mode=dysmsapi 时生效）。
 *
 * <p>当前为预留实现：签名需要企业资质审核，个人主体暂不可用；
 * 将来资质下来后把 sms.mode 改为 dysmsapi 即可切换，业务代码零改动。
 * 验证码由本服务生成（基类负责），渠道只负责发送，核验走 Redis（基类负责）。
 */
@Slf4j
@Component
@ConditionalOnProperty(name = "sms.mode", havingValue = "dysmsapi")
public class AliyunDysmsCodeChannelImpl extends AbstractRedisCodeChannel {

  /** 阿里云短信服务端点（国内站固定域名） */
  private static final String ENDPOINT = "dysmsapi.aliyuncs.com";

  /** 渠道业务成功码：阿里云约定 body.code == "OK" 才算发送成功 */
  private static final String SUCCESS_CODE = "OK";

  private final SmsProperties smsProperties;

  /** SDK 客户端线程安全，应用内复用一个实例即可 */
  private Client client;

  public AliyunDysmsCodeChannelImpl(StringRedisTemplate stringRedisTemplate, SmsProperties smsProperties) {
    super(stringRedisTemplate, smsProperties);
    this.smsProperties = smsProperties;
  }

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
        .setEndpoint(ENDPOINT)
        // 服务器规约【强制】：调用远程操作必须有超时设置
        .setConnectTimeout(3000)
        .setReadTimeout(5000);
    client = new Client(config);
    log.info("阿里云短信（Dysmsapi）客户端初始化完成 endpoint={}", ENDPOINT);
  }

  @Override
  protected String doSend(String phone, SmsSceneEnum scene, String code) {
    // 验证码是纯数字，手工拼 JSON 无注入风险；以后模板参数多了再抽 Map + 序列化
    SendSmsRequest request = new SendSmsRequest()
        .setPhoneNumbers(phone)
        .setSignName(smsProperties.getSignName())
        .setTemplateCode(smsProperties.getTemplateCode())
        .setTemplateParam("{\"code\":\"" + code + "\"}");
    try {
      SendSmsResponse response = client.sendSms(request);
      String bizCode = response.getBody().getCode();
      // 注意：HTTP 200 不代表发送成功，必须校验业务成功码 "OK"
      if (!SUCCESS_CODE.equals(bizCode)) {
        // 错误信息带上渠道错误码（isv.BUSINESS_LIMIT_CONTROL 等）便于排查
        throw new BizException(ErrorCode.SMS_SEND_FAILED, "短信发送失败: " + bizCode);
      }
      return response.getBody().getBizId();
    } catch (BizException e) {
      // 业务异常原样抛出，不能被下面的兜底 catch 再包一层
      throw e;
    } catch (Exception e) {
      // SDK 方法声明 throws Exception（网络超时、鉴权失败等），统一转义为业务异常
      log.error("Dysmsapi 短信发送异常 phone={}", PhoneUtils.mask(phone), e);
      throw new BizException(ErrorCode.SMS_SEND_FAILED, "短信发送失败，请稍后再试");
    }
  }

}

package com.wechat.www.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.wechat.www.common.constant.RedisKeyConst;
import com.wechat.www.common.constant.SmsConst;
import com.wechat.www.common.exception.BizException;
import com.wechat.www.common.exception.ErrorCode;
import com.wechat.www.common.util.PhoneUtils;
import com.wechat.www.config.SmsProperties;
import com.wechat.www.domain.SmsRecordDO;
import com.wechat.www.domain.dto.SendSmsCodeDTO;
import com.wechat.www.domain.dto.SmsSendResultDTO;
import com.wechat.www.domain.dto.VerifySmsCodeDTO;
import com.wechat.www.domain.enums.SmsSceneEnum;
import com.wechat.www.manager.SmsCodeChannel;
import com.wechat.www.mapper.SmsRecordMapper;
import com.wechat.www.service.SmsService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDate;

/**
 * 短信服务实现。
 *
 * <p>验证码的生成/存储/核验由渠道实现（SmsCodeChannel）负责：
 * auth 模式下阿里云托管，dysmsapi 模式下本服务生成并经 Redis 存取；
 * 本类负责发送频控、核验失败次数限制与发送审计，切换渠道模式（sms.mode）本类零改动。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SmsServiceImpl implements SmsService {

  private final SmsRecordMapper smsRecordMapper;
  private final StringRedisTemplate stringRedisTemplate;
  private final SmsProperties smsProperties;
  private final SmsCodeChannel smsCodeChannel;

  @Override
  public void sendCode(SendSmsCodeDTO dto, String ip) {
    SmsSceneEnum scene = SmsSceneEnum.of(dto.getScene());
    String phone = dto.getPhone();

    // ---------- 频控一：每场景 60 秒冷却。SETNX 原子抢占，并发重复点击天然只有一个能进 ----------
    String limitKey = RedisKeyConst.SMS_LIMIT_PREFIX + scene.getCode() + ":" + phone;
    Boolean acquired = stringRedisTemplate
      .opsForValue()
      .setIfAbsent(limitKey, "1", Duration.ofSeconds(smsProperties.getSendIntervalSeconds()));
    if (!Boolean.TRUE.equals(acquired)) {
      throw new BizException(ErrorCode.SMS_SEND_FREQUENT, "发送太频繁，请稍后再试");
    }

    // ---------- 频控二：当日发送上限。走 idx_phone_create (phone, create_time) 索引 ----------
    Long todayCount = smsRecordMapper.selectCount(new LambdaQueryWrapper<SmsRecordDO>()
      .eq(SmsRecordDO::getPhone, phone)
      .ge(SmsRecordDO::getCreateTime, LocalDate
        .now()
        .atStartOfDay()));
    if (todayCount >= smsProperties.getDailyLimit()) {
      throw new BizException(ErrorCode.SMS_SEND_FREQUENT, "今日发送次数已达上限");
    }

    // ---------- 渠道发送（验证码生成与托管由渠道实现负责，本类不感知） ----------
    // 记录先建壳：create_time/update_time 不赋值，交给数据库 DEFAULT CURRENT_TIMESTAMP 填充
    SmsRecordDO record = new SmsRecordDO();
    record.setPhone(phone);
    record.setScene(scene.getCode());
    record.setIp(ip);
    record.setDeviceId(dto.getDeviceId());

    SmsSendResultDTO result;
    try {
      result = smsCodeChannel.sendCode(phone, scene);
    } catch (BizException e) {
      record.setErrorCode(e.getErrorCode().getCode());
      record.setSendStatus(SmsConst.SEND_STATUS_FAIL);
      smsRecordMapper.insert(record);
      // 冷却 key 不回滚：失败也占住窗口，避免攻击者借"必失败请求"绕过频控反复打接口
      throw e;
    }
    record.setBizId(result.bizId());
    record.setRequestId(result.requestId());
    record.setSendStatus(SmsConst.SEND_STATUS_SUCCESS);
    smsRecordMapper.insert(record);

    // 新验证码已生效：清除旧的核验失败计数，尝试次数随验证码重新计算
    stringRedisTemplate.delete(verifyFailKey(scene, phone));

    log.info("短信验证码发送成功 phone={} scene={} bizId={} requestId={}", PhoneUtils.mask(phone),
        scene.getCode(), result.bizId(), result.requestId());
  }

  @Override
  public void verifyCode(VerifySmsCodeDTO dto) {
    SmsSceneEnum scene = SmsSceneEnum.of(dto.getScene());
    String phone = dto.getPhone();
    String failKey = verifyFailKey(scene, phone);

    // 先查失败次数，达到上限直接拒绝，不再调用渠道：
    // auth 模式验证码托管在阿里云，本地无法主动作废，只能在入口拦截，直到用户重新获取验证码
    String failCount = stringRedisTemplate
      .opsForValue()
      .get(failKey);
    if (failCount != null && Integer.parseInt(failCount) >= SmsConst.VERIFY_MAX_FAIL_COUNT) {
      throw new BizException(ErrorCode.SMS_CODE_ATTEMPT_LIMIT, "验证码错误次数过多，请重新获取");
    }

    // 渠道核验：auth 调阿里云校验，dysmsapi 比对 Redis；通过时渠道侧自行作废验证码防重放
    if (!smsCodeChannel.verifyCode(phone, scene, dto.getCode())) {
      // 失败计数 +1，TTL 与验证码有效期对齐：验证码过期后计数自然失效
      stringRedisTemplate
        .opsForValue()
        .increment(failKey);
      stringRedisTemplate.expire(failKey, Duration.ofSeconds(smsProperties.getCodeExpireSeconds()));
      throw new BizException(ErrorCode.SMS_CODE_ERROR, "验证码错误或已过期");
    }

    // 核验通过：清除失败计数，不影响该号码下一次验证码
    stringRedisTemplate.delete(failKey);
    log.info("短信验证码核验通过 phone={} scene={}", PhoneUtils.mask(phone), scene.getCode());
  }

  /**
   * 核验失败计数的 Redis key：wechat:sms:verify:fail:{scene}:{phone}
   */
  private String verifyFailKey(SmsSceneEnum scene, String phone) {
    return RedisKeyConst.SMS_VERIFY_FAIL_PREFIX + scene.getCode() + ":" + phone;
  }

}

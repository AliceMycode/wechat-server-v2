package com.wechat.www.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.wechat.www.common.constant.RedisKeyConst;
import com.wechat.www.common.exception.BizException;
import com.wechat.www.common.exception.ErrorCode;
import com.wechat.www.common.util.PhoneUtils;
import com.wechat.www.config.SmsProperties;
import com.wechat.www.domain.SmsRecordDO;
import com.wechat.www.domain.dto.SendSmsCodeDTO;
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
 * auth 模式下阿里云托管，dysmsapi/mock 模式下本服务生成并经 Redis 存取；
 * 本类只负责频控与发送审计，切换渠道模式（sms.mode）本类零改动。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SmsServiceImpl implements SmsService {

  /** sms_record.send_status：发送成功 */
  private static final int SEND_STATUS_SUCCESS = 1;

  /** sms_record.send_status：发送失败 */
  private static final int SEND_STATUS_FAIL = 2;

  private final SmsRecordMapper smsRecordMapper;
  private final StringRedisTemplate stringRedisTemplate;
  private final SmsProperties smsProperties;
  private final SmsCodeChannel smsCodeChannel;

  @Override
  public void sendCode(SendSmsCodeDTO dto, String ip) {
    SmsSceneEnum scene = SmsSceneEnum.of(dto.getScene());
    String phone = dto.getPhone();

    // ---------- 频控一：60 秒冷却。SETNX 原子抢占，并发重复点击天然只有一个能进 ----------
    String limitKey = RedisKeyConst.SMS_LIMIT_PREFIX + phone;
    Boolean acquired = stringRedisTemplate.opsForValue()
        .setIfAbsent(limitKey, "1", Duration.ofSeconds(smsProperties.getSendIntervalSeconds()));
    if (!Boolean.TRUE.equals(acquired)) {
      throw new BizException(ErrorCode.SMS_SEND_FREQUENT, "发送太频繁，请稍后再试");
    }

    // ---------- 频控二：当日发送上限。走 idx_phone_create (phone, create_time) 索引 ----------
    Long todayCount = smsRecordMapper.selectCount(new LambdaQueryWrapper<SmsRecordDO>()
        .eq(SmsRecordDO::getPhone, phone)
        .ge(SmsRecordDO::getCreateTime, LocalDate.now().atStartOfDay()));
    if (todayCount >= smsProperties.getDailyLimit()) {
      throw new BizException(ErrorCode.SMS_SEND_FREQUENT, "今日发送次数已达上限");
    }

    // ---------- 渠道发送（验证码生成与托管由渠道实现负责，本类不感知） ----------
    String bizId;
    try {
      bizId = smsCodeChannel.sendCode(phone, scene);
    } catch (BizException e) {
      saveRecord(phone, scene, null, e.getErrorCode().getCode(), SEND_STATUS_FAIL, ip);
      // 冷却 key 不回滚：失败也占住窗口，避免攻击者借"必失败请求"绕过频控反复打接口
      throw e;
    }
    saveRecord(phone, scene, bizId, null, SEND_STATUS_SUCCESS, ip);

    log.info("短信验证码发送成功 phone={} scene={} bizId={}", PhoneUtils.mask(phone), scene.getCode(), bizId);
  }

  /**
   * 落一条发送记录。create_time/update_time 不赋值：MP 默认跳过 null 字段，
   * 交给数据库 DEFAULT CURRENT_TIMESTAMP 填充。
   */
  private void saveRecord(String phone, SmsSceneEnum scene, String bizId,
      String errorCode, int sendStatus, String ip) {
    SmsRecordDO record = new SmsRecordDO();
    record.setPhone(phone);
    record.setScene(scene.getCode());
    record.setBizId(bizId);
    record.setErrorCode(errorCode);
    record.setSendStatus(sendStatus);
    record.setIp(ip);
    smsRecordMapper.insert(record);
  }

}

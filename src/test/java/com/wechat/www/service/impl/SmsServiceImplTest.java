package com.wechat.www.service.impl;

import com.wechat.www.common.constant.RedisKeyConst;
import com.wechat.www.common.constant.SmsConst;
import com.wechat.www.common.exception.BizException;
import com.wechat.www.common.exception.ErrorCodeEnum;
import com.wechat.www.config.SmsProperties;
import com.wechat.www.domain.SmsRecordDO;
import com.wechat.www.domain.dto.SendSmsCodeDTO;
import com.wechat.www.domain.dto.SmsSendResultDTO;
import com.wechat.www.domain.dto.VerifySmsCodeDTO;
import com.wechat.www.domain.enums.SmsSceneEnum;
import com.wechat.www.manager.SmsCodeChannel;
import com.wechat.www.mapper.SmsRecordMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * 短信服务单元测试：频控、核验失败计数与落库状态。
 *
 * <p>纯 Mockito 单测，不启动 Spring、不连 MySQL/Redis，所以能精确覆盖
 * "60 秒冷却"、"第 5 次失败" 这类靠人工等待根本测不到的分支。
 */
@ExtendWith(MockitoExtension.class)
class SmsServiceImplTest {

  private static final String PHONE = "13800138000";
  private static final String REGISTER = "register";
  private static final String IP = "127.0.0.1";
  private static final String DEVICE_ID = "device-1";

  private static final String LIMIT_KEY = RedisKeyConst.SMS_LIMIT_PREFIX + REGISTER + ":" + PHONE;
  private static final String FAIL_KEY = RedisKeyConst.SMS_VERIFY_FAIL_PREFIX + REGISTER + ":" + PHONE;

  @Mock
  private SmsRecordMapper smsRecordMapper;

  @Mock
  private StringRedisTemplate stringRedisTemplate;

  @Mock
  private ValueOperations<String, String> valueOperations;

  @Mock
  private SmsCodeChannel smsCodeChannel;

  private SmsServiceImpl smsService;

  @BeforeEach
  void setUp() {
    SmsProperties smsProperties = new SmsProperties();
    smsProperties.setSendIntervalSeconds(60);
    smsProperties.setDailyLimit(10);
    smsProperties.setCodeExpireSeconds(300);
    smsService = new SmsServiceImpl(smsRecordMapper, stringRedisTemplate, smsProperties, smsCodeChannel);
  }

  // ==================== sendCode：频控 ====================

  @Test
  @DisplayName("冷却未过：拒绝发送，不调渠道也不落库")
  void sendCode_whenWithinCooldown_shouldRejectWithoutTouchingChannel() {
    when(stringRedisTemplate.opsForValue()).thenReturn(valueOperations);
    when(valueOperations.setIfAbsent(eq(LIMIT_KEY), eq("1"),
      any(Duration.class))).thenReturn(false);

    BizException ex = assertThrows(BizException.class, () -> smsService.sendCode(sendDto(), IP));

    assertThat(ex.getErrorCode()).isEqualTo(ErrorCodeEnum.REQUEST_FREQUENT);
    verify(smsCodeChannel, never()).sendCode(anyString(), any());
    verify(smsRecordMapper, never()).insert(any(SmsRecordDO.class));
  }

  @Test
  @DisplayName("当日发送达上限：拒绝发送")
  void sendCode_whenDailyLimitReached_shouldReject() {
    when(stringRedisTemplate.opsForValue()).thenReturn(valueOperations);
    when(valueOperations.setIfAbsent(anyString(), anyString(), any(Duration.class))).thenReturn(true);
    when(smsRecordMapper.selectCount(any())).thenReturn(10L);

    BizException ex = assertThrows(BizException.class, () -> smsService.sendCode(sendDto(), IP));

    assertThat(ex.getErrorCode()).isEqualTo(ErrorCodeEnum.REQUEST_FREQUENT);
    assertThat(ex.getMessage()).isEqualTo("今日发送次数已达上限");
    verify(smsCodeChannel, never()).sendCode(anyString(), any());
  }

  // ==================== sendCode：落库 ====================

  @Test
  @DisplayName("发送成功：落库成功状态并带回 bizId/requestId，清除失败计数")
  void sendCode_whenChannelSucceeds_shouldPersistSuccessRecord() {
    when(stringRedisTemplate.opsForValue()).thenReturn(valueOperations);
    when(valueOperations.setIfAbsent(anyString(), anyString(), any(Duration.class))).thenReturn(true);
    when(smsRecordMapper.selectCount(any())).thenReturn(0L);
    when(smsCodeChannel.sendCode(PHONE, SmsSceneEnum.REGISTER)).thenReturn(new SmsSendResultDTO("biz-1", "req-1"));

    smsService.sendCode(sendDto(), IP);

    ArgumentCaptor<SmsRecordDO> captor = ArgumentCaptor.forClass(SmsRecordDO.class);
    verify(smsRecordMapper).insert(captor.capture());
    SmsRecordDO saved = captor.getValue();
    assertThat(saved.getPhone()).isEqualTo(PHONE);
    assertThat(saved.getScene()).isEqualTo(REGISTER);
    assertThat(saved.getIp()).isEqualTo(IP);
    assertThat(saved.getDeviceId()).isEqualTo(DEVICE_ID);
    assertThat(saved.getBizId()).isEqualTo("biz-1");
    assertThat(saved.getRequestId()).isEqualTo("req-1");
    assertThat(saved.getSendStatus()).isEqualTo(SmsConst.SEND_STATUS_SUCCESS);
    // 失败计数清零，并把 TTL 锚定在发送时刻（与验证码同生命周期）
    verify(valueOperations).set(FAIL_KEY, "0", Duration.ofSeconds(300));
  }

  @Test
  @DisplayName("渠道失败：落库失败状态与错误码，异常原样透传")
  void sendCode_whenChannelFails_shouldPersistFailRecordAndRethrow() {
    when(stringRedisTemplate.opsForValue()).thenReturn(valueOperations);
    when(valueOperations.setIfAbsent(anyString(), anyString(), any(Duration.class))).thenReturn(true);
    when(smsRecordMapper.selectCount(any())).thenReturn(0L);
    when(smsCodeChannel.sendCode(PHONE, SmsSceneEnum.REGISTER)).thenThrow(new BizException(ErrorCodeEnum.SMS_SEND_FAILED, "短信发送失败: isv.BUSINESS_LIMIT_CONTROL"));

    BizException ex = assertThrows(BizException.class, () -> smsService.sendCode(sendDto(), IP));

    assertThat(ex.getErrorCode()).isEqualTo(ErrorCodeEnum.SMS_SEND_FAILED);
    ArgumentCaptor<SmsRecordDO> captor = ArgumentCaptor.forClass(SmsRecordDO.class);
    verify(smsRecordMapper).insert(captor.capture());
    assertThat(captor.getValue().getSendStatus()).isEqualTo(SmsConst.SEND_STATUS_FAIL);
    assertThat(captor.getValue().getErrorCode()).isEqualTo(ErrorCodeEnum.SMS_SEND_FAILED.getCode());
    // 发送失败不重设失败计数：旧验证码仍然有效，计数窗口不应被延长或清零
    verify(valueOperations, never()).set(anyString(), anyString(), any(Duration.class));
  }

  // ==================== verifyCode：失败计数 ====================

  @Test
  @DisplayName("尝试次数超上限：直接拒绝，不再调用渠道")
  void verifyCode_whenAttemptLimitExceeded_shouldRejectWithoutChannel() {
    when(stringRedisTemplate.hasKey(FAIL_KEY)).thenReturn(true);
    when(stringRedisTemplate.opsForValue()).thenReturn(valueOperations);
    when(valueOperations.increment(FAIL_KEY)).thenReturn(SmsConst.VERIFY_MAX_FAIL_COUNT + 1L);

    BizException ex = assertThrows(BizException.class, () -> smsService.verifyCode(verifyDto("123456")));

    assertThat(ex.getErrorCode()).isEqualTo(ErrorCodeEnum.SMS_CODE_ATTEMPT_LIMIT);
    verify(smsCodeChannel, never()).verifyCode(anyString(), any(), anyString());
  }

  @Test
  @DisplayName("核验不通过：本次尝试已计入，且不续期 TTL")
  void verifyCode_whenCodeWrong_shouldKeepAttemptCountWithoutRenewingTtl() {
    when(stringRedisTemplate.hasKey(FAIL_KEY)).thenReturn(true);
    when(stringRedisTemplate.opsForValue()).thenReturn(valueOperations);
    when(valueOperations.increment(FAIL_KEY)).thenReturn(3L);
    when(smsCodeChannel.verifyCode(PHONE, SmsSceneEnum.REGISTER, "123456")).thenReturn(false);

    BizException ex = assertThrows(BizException.class, () -> smsService.verifyCode(verifyDto("123456")));

    assertThat(ex.getErrorCode()).isEqualTo(ErrorCodeEnum.SMS_CODE_ERROR);
    verify(valueOperations).increment(FAIL_KEY);
    // TTL 由发送时设定，失败续期会让计数比验证码活得久
    verify(stringRedisTemplate, never()).expire(anyString(), any(Duration.class));
    verify(stringRedisTemplate, never()).delete(FAIL_KEY);
  }

  @Test
  @DisplayName("验证码已过期（计数 key 已消失）：直接拒绝，不碰 Redis 计数")
  void verifyCode_whenFailKeyAbsent_shouldRejectWithoutIncrement() {
    when(stringRedisTemplate.hasKey(FAIL_KEY)).thenReturn(false);

    BizException ex = assertThrows(BizException.class, () -> smsService.verifyCode(verifyDto("123456")));

    assertThat(ex.getErrorCode()).isEqualTo(ErrorCodeEnum.SMS_CODE_ERROR);
    verify(stringRedisTemplate, never()).opsForValue();
    verify(smsCodeChannel, never()).verifyCode(anyString(), any(), anyString());
  }

  @Test
  @DisplayName("核验通过：清除计数")
  void verifyCode_whenCodeCorrect_shouldClearFailCount() {
    when(stringRedisTemplate.hasKey(FAIL_KEY)).thenReturn(true);
    when(stringRedisTemplate.opsForValue()).thenReturn(valueOperations);
    when(valueOperations.increment(FAIL_KEY)).thenReturn(1L);
    when(smsCodeChannel.verifyCode(PHONE, SmsSceneEnum.REGISTER, "123456")).thenReturn(true);

    assertDoesNotThrow(() -> smsService.verifyCode(verifyDto("123456")));

    verify(stringRedisTemplate).delete(FAIL_KEY);
  }

  private SendSmsCodeDTO sendDto() {
    SendSmsCodeDTO dto = new SendSmsCodeDTO();
    dto.setPhone(PHONE);
    dto.setScene(REGISTER);
    dto.setDeviceId(DEVICE_ID);
    return dto;
  }

  private VerifySmsCodeDTO verifyDto(String code) {
    VerifySmsCodeDTO dto = new VerifySmsCodeDTO();
    dto.setPhone(PHONE);
    dto.setScene(REGISTER);
    dto.setCode(code);
    return dto;
  }

}

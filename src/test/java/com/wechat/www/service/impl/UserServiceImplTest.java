package com.wechat.www.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.wechat.www.common.exception.BizException;
import com.wechat.www.common.exception.ErrorCodeEnum;
import com.wechat.www.domain.UserAccountDO;
import com.wechat.www.domain.UserDeviceDO;
import com.wechat.www.domain.UserProfileDO;
import com.wechat.www.domain.dto.RegisterUserDTO;
import com.wechat.www.domain.dto.VerifySmsCodeDTO;
import com.wechat.www.domain.enums.SmsSceneEnum;
import com.wechat.www.domain.vo.RegisterUserVO;
import com.wechat.www.mapper.UserAccountMapper;
import com.wechat.www.mapper.UserDeviceMapper;
import com.wechat.www.mapper.UserProfileMapper;
import com.wechat.www.service.SmsService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.security.crypto.password.PasswordEncoder;

/**
 * 用户注册单元测试：密码策略、验证码核验、手机号查重与三表落库。
 *
 * <p>纯 Mockito 单测，不启动 Spring、不连 MySQL/Redis。
 */
@ExtendWith(MockitoExtension.class)
class UserServiceImplTest {

  private static final String PHONE = "13800138000";
  private static final String VALID_PASSWORD = "Wechat2026";
  private static final String NICKNAME = "微信用户001";
  private static final String CODE = "123456";
  private static final String DEVICE_ID = "device-1";
  private static final String PASSWORD_HASH = "$2a$10$hashed";
  private static final long USER_ID = 726400000000000001L;

  @Mock
  private UserAccountMapper userAccountMapper;

  @Mock
  private UserProfileMapper userProfileMapper;

  @Mock
  private UserDeviceMapper userDeviceMapper;

  @Mock
  private SmsService smsService;

  @Mock
  private PasswordEncoder passwordEncoder;

  private UserServiceImpl userService;

  @BeforeEach
  void setUp() {
    userService = new UserServiceImpl(userAccountMapper, userProfileMapper, userDeviceMapper, smsService,
        passwordEncoder);
  }

  // ==================== 密码策略 ====================

  @Test
  @DisplayName("密码不足 8 位：拒绝，且不消耗验证码")
  void register_whenPasswordTooShort_shouldRejectBeforeVerifyingCode() {
    BizException ex = assertThrows(BizException.class, () -> userService.register(dto("Wc2026")));

    assertThat(ex.getErrorCode()).isEqualTo(ErrorCodeEnum.PASSWORD_LENGTH_INVALID);
    verify(smsService, never()).verifyCode(any());
  }

  @Test
  @DisplayName("密码超过 16 位：拒绝")
  void register_whenPasswordTooLong_shouldReject() {
    BizException ex = assertThrows(BizException.class, () -> userService.register(dto("Wechat2026ABCDEFG")));

    assertThat(ex.getErrorCode()).isEqualTo(ErrorCodeEnum.PASSWORD_LENGTH_INVALID);
  }

  @Test
  @DisplayName("密码只有数字一种字符类型：拒绝")
  void register_whenPasswordSingleCharType_shouldReject() {
    BizException ex = assertThrows(BizException.class, () -> userService.register(dto("12345678")));

    assertThat(ex.getErrorCode()).isEqualTo(ErrorCodeEnum.PASSWORD_STRENGTH_WEAK);
    verify(smsService, never()).verifyCode(any());
  }

  @Test
  @DisplayName("密码含空格：拒绝（只允许 ASCII 可打印字符）")
  void register_whenPasswordContainsSpace_shouldReject() {
    BizException ex = assertThrows(BizException.class, () -> userService.register(dto("Wechat 2026")));

    assertThat(ex.getErrorCode()).isEqualTo(ErrorCodeEnum.PASSWORD_STRENGTH_WEAK);
  }

  // ==================== 验证码与查重 ====================

  @Test
  @DisplayName("验证码核验失败：异常原样透传，不落库")
  void register_whenCodeInvalid_shouldPropagateWithoutPersisting() {
    doThrow(new BizException(ErrorCodeEnum.SMS_CODE_ERROR, "验证码错误或已过期"))
        .when(smsService).verifyCode(any());

    BizException ex = assertThrows(BizException.class, () -> userService.register(dto(VALID_PASSWORD)));

    assertThat(ex.getErrorCode()).isEqualTo(ErrorCodeEnum.SMS_CODE_ERROR);
    verify(userAccountMapper, never()).insert(any(UserAccountDO.class));
    verify(userProfileMapper, never()).insert(any(UserProfileDO.class));
    verify(userDeviceMapper, never()).insert(any(UserDeviceDO.class));
  }

  @Test
  @DisplayName("手机号已注册：拒绝，不落库")
  void register_whenPhoneAlreadyRegistered_shouldReject() {
    when(userAccountMapper.selectCount(any())).thenReturn(1L);

    BizException ex = assertThrows(BizException.class, () -> userService.register(dto(VALID_PASSWORD)));

    assertThat(ex.getErrorCode()).isEqualTo(ErrorCodeEnum.PHONE_ALREADY_REGISTERED);
    verify(userAccountMapper, never()).insert(any(UserAccountDO.class));
  }

  @Test
  @DisplayName("并发撞唯一键：转成手机号已注册，不写资料与设备")
  void register_whenDuplicateKeyOnInsert_shouldConvertToRegistered() {
    when(userAccountMapper.selectCount(any())).thenReturn(0L);
    when(passwordEncoder.encode(VALID_PASSWORD)).thenReturn(PASSWORD_HASH);
    doThrow(new DuplicateKeyException("uk_country_phone"))
        .when(userAccountMapper).insert(any(UserAccountDO.class));

    BizException ex = assertThrows(BizException.class, () -> userService.register(dto(VALID_PASSWORD)));

    assertThat(ex.getErrorCode()).isEqualTo(ErrorCodeEnum.PHONE_ALREADY_REGISTERED);
    verify(userProfileMapper, never()).insert(any(UserProfileDO.class));
    verify(userDeviceMapper, never()).insert(any(UserDeviceDO.class));
  }

  // ==================== 注册成功 ====================

  @Test
  @DisplayName("注册成功：三表写入且 user_id 一致，密码以哈希落库")
  void register_whenSuccess_shouldPersistThreeTablesWithSameUserId() {
    when(userAccountMapper.selectCount(any())).thenReturn(0L);
    when(passwordEncoder.encode(VALID_PASSWORD)).thenReturn(PASSWORD_HASH);
    // 模拟 MyBatis-Plus ASSIGN_ID：insert 时生成雪花 ID 并回填实体
    doAnswer(invocation -> {
      UserAccountDO account = invocation.getArgument(0);
      account.setId(USER_ID);
      return 1;
    }).when(userAccountMapper).insert(any(UserAccountDO.class));

    RegisterUserVO vo = userService.register(dto(VALID_PASSWORD));

    assertThat(vo.getUserId()).isEqualTo(USER_ID);
    assertThat(vo.getNickname()).isEqualTo(NICKNAME);

    // 验证码以 register 场景核验
    ArgumentCaptor<VerifySmsCodeDTO> verifyCaptor = ArgumentCaptor.forClass(VerifySmsCodeDTO.class);
    verify(smsService).verifyCode(verifyCaptor.capture());
    assertThat(verifyCaptor.getValue().getScene()).isEqualTo(SmsSceneEnum.REGISTER.getCode());

    ArgumentCaptor<UserAccountDO> accountCaptor = ArgumentCaptor.forClass(UserAccountDO.class);
    verify(userAccountMapper).insert(accountCaptor.capture());
    UserAccountDO account = accountCaptor.getValue();
    assertThat(account.getPhone()).isEqualTo(PHONE);
    assertThat(account.getCountryCode()).isEqualTo("86");
    assertThat(account.getPasswordHash()).isEqualTo(PASSWORD_HASH);

    ArgumentCaptor<UserProfileDO> profileCaptor = ArgumentCaptor.forClass(UserProfileDO.class);
    verify(userProfileMapper).insert(profileCaptor.capture());
    assertThat(profileCaptor.getValue().getUserId()).isEqualTo(USER_ID);
    assertThat(profileCaptor.getValue().getNickname()).isEqualTo(NICKNAME);

    ArgumentCaptor<UserDeviceDO> deviceCaptor = ArgumentCaptor.forClass(UserDeviceDO.class);
    verify(userDeviceMapper).insert(deviceCaptor.capture());
    assertThat(deviceCaptor.getValue().getUserId()).isEqualTo(USER_ID);
    assertThat(deviceCaptor.getValue().getDeviceId()).isEqualTo(DEVICE_ID);
  }

  private RegisterUserDTO dto(String password) {
    RegisterUserDTO dto = new RegisterUserDTO();
    dto.setPhone(PHONE);
    dto.setPassword(password);
    dto.setNickname(NICKNAME);
    dto.setCode(CODE);
    dto.setDeviceId(DEVICE_ID);
    return dto;
  }

}

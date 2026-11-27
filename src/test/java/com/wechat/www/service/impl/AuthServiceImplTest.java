package com.wechat.www.service.impl;

import com.wechat.www.common.constant.PhoneConst;
import com.wechat.www.common.constant.RedisKeyConst;
import com.wechat.www.common.constant.UserConst;
import com.wechat.www.common.exception.BizException;
import com.wechat.www.common.exception.ErrorCodeEnum;
import com.wechat.www.common.security.LoginUser;
import com.wechat.www.domain.entity.UserAccountDO;
import com.wechat.www.domain.entity.UserDeviceDO;
import com.wechat.www.domain.entity.UserProfileDO;
import com.wechat.www.domain.dto.PasswordLoginDTO;
import com.wechat.www.domain.dto.SmsLoginDTO;
import com.wechat.www.domain.dto.VerifySmsCodeDTO;
import com.wechat.www.domain.enums.SmsSceneEnum;
import com.wechat.www.domain.vo.LoginVO;
import com.wechat.www.domain.vo.UserProfileVO;
import com.wechat.www.manager.TokenManager;
import com.wechat.www.mapper.UserAccountMapper;
import com.wechat.www.mapper.UserDeviceMapper;
import com.wechat.www.mapper.UserProfileMapper;
import com.wechat.www.service.SmsService;
import com.wechat.www.service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * 登录鉴权单元测试：密码登录、验证码登录、账号状态与登出。
 *
 * <p>纯 Mockito 单测，不启动 Spring、不连 MySQL/Redis。
 */
@ExtendWith(MockitoExtension.class)
class AuthServiceImplTest {

  private static final String PHONE = "13800138000";
  private static final String PASSWORD = "Wechat2026";
  private static final String PASSWORD_HASH = "$2a$10$hashed";
  private static final String NICKNAME = "微信用户001";
  private static final String CODE = "123456";
  private static final String DEVICE_ID = "device-1";
  private static final String TOKEN = "token-abc";
  private static final long USER_ID = 726400000000000001L;
  private static final long AVATAR_FILE_ID = 726400000000000002L;
  private static final String SIGNATURE = "这个人很懒，什么都没留下";
  private static final LocalDate BIRTHDAY = LocalDate.of(1995, 6, 1);

  private static final String FAIL_KEY = RedisKeyConst.AUTH_PWD_FAIL_PREFIX + PHONE;

  @Mock
  private UserAccountMapper userAccountMapper;

  @Mock
  private UserProfileMapper userProfileMapper;

  @Mock
  private UserDeviceMapper userDeviceMapper;

  @Mock
  private UserService userService;

  @Mock
  private SmsService smsService;

  @Mock
  private TokenManager tokenManager;

  @Mock
  private PasswordEncoder passwordEncoder;

  @Mock
  private StringRedisTemplate stringRedisTemplate;

  @Mock
  private ValueOperations<String, String> valueOperations;

  private AuthServiceImpl authService;

  @BeforeEach
  void setUp() {
    authService = new AuthServiceImpl(userAccountMapper, userProfileMapper, userDeviceMapper, userService, smsService,
      tokenManager, passwordEncoder, stringRedisTemplate);
  }

  // ==================== 密码登录 ====================

  @Test
  @DisplayName("密码正确：签发 token、更新最后登录时间、清除失败计数")
  void loginByPassword_whenSuccess_shouldIssueTokenAndClearFailCount() {
    when(stringRedisTemplate.opsForValue()).thenReturn(valueOperations);
    when(valueOperations.increment(FAIL_KEY)).thenReturn(1L);
    when(userAccountMapper.selectOne(any())).thenReturn(account(UserConst.STATUS_NORMAL));
    when(passwordEncoder.matches(PASSWORD, PASSWORD_HASH)).thenReturn(true);
    when(userDeviceMapper.selectOne(any())).thenReturn(device());
    when(tokenManager.issue(USER_ID, DEVICE_ID)).thenReturn(TOKEN);

    LoginVO vo = authService.loginByPassword(passwordDto());

    assertThat(vo.getToken()).isEqualTo(TOKEN);
    verify(userAccountMapper).updateById(any(UserAccountDO.class));
    verify(userDeviceMapper).updateById(any(UserDeviceDO.class));
    verify(stringRedisTemplate).delete(FAIL_KEY);
  }

  @Test
  @DisplayName("账号不存在：统一提示手机号或密码错误，不暴露注册状态")
  void loginByPassword_whenAccountMissing_shouldReturnUnifiedMessage() {
    when(stringRedisTemplate.opsForValue()).thenReturn(valueOperations);
    when(valueOperations.increment(FAIL_KEY)).thenReturn(1L);
    when(userAccountMapper.selectOne(any())).thenReturn(null);

    BizException ex = assertThrows(BizException.class, () -> authService.loginByPassword(passwordDto()));

    assertThat(ex.getErrorCode()).isEqualTo(ErrorCodeEnum.LOGIN_FAILED);
    verify(tokenManager, never()).issue(any(), any());
  }

  @Test
  @DisplayName("密码错误：返回同一错误码，不清除失败计数")
  void loginByPassword_whenPasswordWrong_shouldKeepFailCount() {
    when(stringRedisTemplate.opsForValue()).thenReturn(valueOperations);
    when(valueOperations.increment(FAIL_KEY)).thenReturn(2L);
    when(userAccountMapper.selectOne(any())).thenReturn(account(UserConst.STATUS_NORMAL));
    when(passwordEncoder.matches(PASSWORD, PASSWORD_HASH)).thenReturn(false);

    BizException ex = assertThrows(BizException.class, () -> authService.loginByPassword(passwordDto()));

    assertThat(ex.getErrorCode()).isEqualTo(ErrorCodeEnum.LOGIN_FAILED);
    verify(stringRedisTemplate, never()).delete(FAIL_KEY);
    verify(tokenManager, never()).issue(any(), any());
  }

  @Test
  @DisplayName("尝试次数超限：直接拒绝，不查库也不签 token")
  void loginByPassword_whenAttemptLimitExceeded_shouldReject() {
    when(stringRedisTemplate.opsForValue()).thenReturn(valueOperations);
    when(valueOperations.increment(FAIL_KEY)).thenReturn(6L);

    BizException ex = assertThrows(BizException.class, () -> authService.loginByPassword(passwordDto()));

    assertThat(ex.getErrorCode()).isEqualTo(ErrorCodeEnum.PASSWORD_FAIL_LIMIT);
    verify(userAccountMapper, never()).selectOne(any());
    verify(tokenManager, never()).issue(any(), any());
  }

  @Test
  @DisplayName("账号被禁用：拒绝登录")
  void loginByPassword_whenAccountDisabled_shouldReject() {
    when(stringRedisTemplate.opsForValue()).thenReturn(valueOperations);
    when(valueOperations.increment(FAIL_KEY)).thenReturn(1L);
    when(userAccountMapper.selectOne(any())).thenReturn(account(UserConst.STATUS_DISABLED));

    BizException ex = assertThrows(BizException.class, () -> authService.loginByPassword(passwordDto()));

    assertThat(ex.getErrorCode()).isEqualTo(ErrorCodeEnum.ACCOUNT_FROZEN);
  }

  @Test
  @DisplayName("账号已注销：拒绝登录")
  void loginByPassword_whenAccountCancelled_shouldReject() {
    when(stringRedisTemplate.opsForValue()).thenReturn(valueOperations);
    when(valueOperations.increment(FAIL_KEY)).thenReturn(1L);
    when(userAccountMapper.selectOne(any())).thenReturn(account(UserConst.STATUS_CANCELLED));

    BizException ex = assertThrows(BizException.class, () -> authService.loginByPassword(passwordDto()));

    assertThat(ex.getErrorCode()).isEqualTo(ErrorCodeEnum.ACCOUNT_CANCELLED);
  }

  // ==================== 短信登录 ====================

  @Test
  @DisplayName("验证码登录：以 scene=login 核验并签发 token")
  void loginBySms_whenSuccess_shouldVerifyWithLoginScene() {
    when(userAccountMapper.selectOne(any())).thenReturn(account(UserConst.STATUS_NORMAL));
    when(userDeviceMapper.selectOne(any())).thenReturn(device());
    when(tokenManager.issue(USER_ID, DEVICE_ID)).thenReturn(TOKEN);

    LoginVO vo = authService.loginBySms(smsDto());

    assertThat(vo.getToken()).isEqualTo(TOKEN);
    ArgumentCaptor<VerifySmsCodeDTO> captor = ArgumentCaptor.forClass(VerifySmsCodeDTO.class);
    verify(smsService).verifyCode(captor.capture());
    assertThat(captor.getValue().getScene()).isEqualTo(SmsSceneEnum.LOGIN.getCode());
  }

  @Test
  @DisplayName("验证码登录：号码未注册时明确提示，不签 token")
  void loginBySms_whenAccountMissing_shouldReturnNotFound() {
    when(userAccountMapper.selectOne(any())).thenReturn(null);

    BizException ex = assertThrows(BizException.class, () -> authService.loginBySms(smsDto()));

    assertThat(ex.getErrorCode()).isEqualTo(ErrorCodeEnum.ACCOUNT_NOT_FOUND);
    verify(tokenManager, never()).issue(any(), any());
  }

  // ==================== 登出与当前用户 ====================

  @Test
  @DisplayName("登出：只作废当前设备会话")
  void logout_shouldRevokeCurrentDevice() {
    LoginUser loginUser = new LoginUser(USER_ID, DEVICE_ID);

    assertDoesNotThrow(() -> authService.logout(loginUser));

    verify(tokenManager).revoke(USER_ID, DEVICE_ID);
  }

  @Test
  @DisplayName("当前用户：从 user_profile 返回完整资料")
  void currentUser_shouldReturnProfile() {
    when(userProfileMapper.selectOne(any())).thenReturn(profile());

    UserProfileVO vo = authService.currentUser(new LoginUser(USER_ID, DEVICE_ID));

    assertThat(vo.getUserId()).isEqualTo(USER_ID);
    assertThat(vo.getNickname()).isEqualTo(NICKNAME);
    assertThat(vo.getAvatarFileId()).isEqualTo(AVATAR_FILE_ID);
    assertThat(vo.getSignature()).isEqualTo(SIGNATURE);
    assertThat(vo.getGender()).isEqualTo(1);
    assertThat(vo.getArea()).isEqualTo("北京");
    assertThat(vo.getBirthday()).isEqualTo(BIRTHDAY);
  }

  @Test
  @DisplayName("当前用户：资料已被删除时按登录失效处理")
  void currentUser_whenProfileMissing_shouldReturnLoginExpired() {
    when(userProfileMapper.selectOne(any())).thenReturn(null);

    BizException ex = assertThrows(BizException.class, () -> authService.currentUser(new LoginUser(USER_ID,
      DEVICE_ID)));

    assertThat(ex.getErrorCode()).isEqualTo(ErrorCodeEnum.LOGIN_EXPIRED);
  }

  // ==================== 测试数据 ====================

  private UserAccountDO account(int status) {
    UserAccountDO account = new UserAccountDO();
    account.setId(USER_ID);
    account.setCountryCode(PhoneConst.DEFAULT_COUNTRY_CODE);
    account.setPhone(PHONE);
    account.setPasswordHash(PASSWORD_HASH);
    account.setStatus(status);
    return account;
  }

  private UserProfileDO profile() {
    UserProfileDO profile = new UserProfileDO();
    profile.setUserId(USER_ID);
    profile.setNickname(NICKNAME);
    profile.setAvatarFileId(AVATAR_FILE_ID);
    profile.setSignature(SIGNATURE);
    profile.setGender(1);
    profile.setArea("北京");
    profile.setBirthday(BIRTHDAY);
    return profile;
  }

  private UserDeviceDO device() {
    UserDeviceDO device = new UserDeviceDO();
    device.setId(1L);
    device.setUserId(USER_ID);
    device.setDeviceId(DEVICE_ID);
    return device;
  }

  private PasswordLoginDTO passwordDto() {
    PasswordLoginDTO dto = new PasswordLoginDTO();
    dto.setPhone(PHONE);
    dto.setPassword(PASSWORD);
    dto.setDeviceId(DEVICE_ID);
    return dto;
  }

  private SmsLoginDTO smsDto() {
    SmsLoginDTO dto = new SmsLoginDTO();
    dto.setPhone(PHONE);
    dto.setCode(CODE);
    dto.setDeviceId(DEVICE_ID);
    return dto;
  }

}

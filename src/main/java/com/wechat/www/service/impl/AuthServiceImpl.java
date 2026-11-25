package com.wechat.www.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.wechat.www.common.constant.RedisKeyConst;
import com.wechat.www.common.constant.UserConst;
import com.wechat.www.common.exception.BizException;
import com.wechat.www.common.exception.ErrorCodeEnum;
import com.wechat.www.common.security.LoginUser;
import com.wechat.www.common.util.PhoneUtils;
import com.wechat.www.config.AuthProperties;
import com.wechat.www.domain.UserAccountDO;
import com.wechat.www.domain.UserDeviceDO;
import com.wechat.www.domain.UserProfileDO;
import com.wechat.www.domain.dto.PasswordLoginDTO;
import com.wechat.www.domain.dto.RegisterUserDTO;
import com.wechat.www.domain.dto.SmsLoginDTO;
import com.wechat.www.domain.dto.VerifySmsCodeDTO;
import com.wechat.www.domain.enums.SmsSceneEnum;
import com.wechat.www.domain.vo.LoginVO;
import com.wechat.www.domain.vo.RegisterUserVO;
import com.wechat.www.domain.vo.UserInfoVO;
import com.wechat.www.manager.TokenManager;
import com.wechat.www.mapper.UserAccountMapper;
import com.wechat.www.mapper.UserDeviceMapper;
import com.wechat.www.mapper.UserProfileMapper;
import com.wechat.www.service.AuthService;
import com.wechat.www.service.SmsService;
import com.wechat.www.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDateTime;

/**
 * 登录鉴权实现。
 *
 * <p>所有登录入口最终都汇聚到 {@link #issue}：签发 token → 更新最后登录时间 → 落设备。
 * 注册即登录复用同一出口，保证三种入口签出的会话形态完全一致。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

  private final UserAccountMapper userAccountMapper;
  private final UserProfileMapper userProfileMapper;
  private final UserDeviceMapper userDeviceMapper;
  private final UserService userService;
  private final SmsService smsService;
  private final TokenManager tokenManager;
  private final PasswordEncoder passwordEncoder;
  private final StringRedisTemplate stringRedisTemplate;
  private final AuthProperties authProperties;

  @Override
  public LoginVO registerAndLogin(RegisterUserDTO dto) {
    // userService.register 自带事务，返回时注册已提交；
    // 本方法刻意不加 @Transactional：会话写 Redis 无法参与数据库回滚，
    // 放在事务里只会造成"Redis 成功、数据库回滚"的不一致
    RegisterUserVO user = userService.register(dto);
    return issue(user.getUserId(), dto.getDeviceId(), user.getNickname(), dto.getDeviceType(), dto.getDeviceName());
  }

  @Override
  public LoginVO loginByPassword(PasswordLoginDTO dto) {
    String phone = dto.getPhone();
    String failKey = RedisKeyConst.AUTH_PWD_FAIL_PREFIX + phone;

    // 先占坑再验证：INCR 是单命令原子操作，返回值即本次尝试序号。
    // 并发请求各自拿到不同序号，不会出现"一起读到 4 全部放行"的窗口。
    Long attempt = stringRedisTemplate.opsForValue().increment(failKey);
    if (attempt != null && attempt == 1L) {
      // 只在首次创建时设 TTL，后续失败不续期：否则持续重试会把锁定时间无限延长
      stringRedisTemplate.expire(failKey, Duration.ofSeconds(authProperties.getPasswordLockSeconds()));
    }
    if (attempt != null && attempt > authProperties.getPasswordMaxFailCount()) {
      throw new BizException(ErrorCodeEnum.PASSWORD_FAIL_LIMIT);
    }

    UserAccountDO account = findByPhone(resolveCountryCode(dto.getCountryCode()), phone);
    // 账号不存在与密码错误给同一句提示，防止有人拿登录接口批量探测哪些手机号注册过
    if (account == null) {
      throw new BizException(ErrorCodeEnum.LOGIN_FAILED);
    }
    ensureAccountUsable(account);

    if (!passwordEncoder.matches(dto.getPassword(), account.getPasswordHash())) {
      throw new BizException(ErrorCodeEnum.LOGIN_FAILED);
    }

    // 登录成功清零，下一次失败重新从 1 开始
    stringRedisTemplate.delete(failKey);
    return issue(account.getId(), dto.getDeviceId(), loadNickname(account.getId()), dto.getDeviceType(),
      dto.getDeviceName());
  }

  @Override
  public LoginVO loginBySms(SmsLoginDTO dto) {
    // 验证码先于查账号：未持有验证码的人无法借此探测手机号是否已注册
    VerifySmsCodeDTO verifyDto = new VerifySmsCodeDTO();
    verifyDto.setPhone(dto.getPhone());
    verifyDto.setScene(SmsSceneEnum.LOGIN.getCode());
    verifyDto.setCode(dto.getCode());
    smsService.verifyCode(verifyDto);

    // 走到这里号码归属已被验证码证明，可以明确告知"未注册"
    UserAccountDO account = findByPhone(resolveCountryCode(dto.getCountryCode()), dto.getPhone());
    if (account == null) {
      throw new BizException(ErrorCodeEnum.ACCOUNT_NOT_FOUND);
    }
    ensureAccountUsable(account);

    return issue(account.getId(), dto.getDeviceId(), loadNickname(account.getId()), dto.getDeviceType(),
      dto.getDeviceName());
  }

  @Override
  public void logout(LoginUser loginUser) {
    tokenManager.revoke(loginUser.userId(), loginUser.deviceId());
    log.info("用户登出 userId={} deviceId={}", loginUser.userId(), loginUser.deviceId());
  }

  @Override
  public UserInfoVO currentUser(LoginUser loginUser) {
    UserAccountDO account = userAccountMapper.selectById(loginUser.userId());
    if (account == null) {
      // 账号在会话有效期内被物理删除，按登录失效处理
      throw new BizException(ErrorCodeEnum.LOGIN_EXPIRED);
    }
    return new UserInfoVO(account.getId(), loadNickname(account.getId()), PhoneUtils.mask(account.getPhone()),
      loginUser.deviceId());
  }

  /**
   * 三种登录入口共用的会话签发出口。
   */
  private LoginVO issue(Long userId, String deviceId, String nickname, Integer deviceType, String deviceName) {
    String token = tokenManager.issue(userId, deviceId);
    updateLastLoginTime(userId);
    touchDevice(userId, deviceId, deviceType, deviceName);
    log.info("登录成功 userId={} deviceId={}", userId, deviceId);
    return new LoginVO(token, userId, nickname, deviceId, authProperties.getTokenExpireSeconds());
  }

  private UserAccountDO findByPhone(String countryCode, String phone) {
    return userAccountMapper.selectOne(
      new LambdaQueryWrapper<UserAccountDO>()
        .eq(UserAccountDO::getCountryCode, countryCode)
        .eq(UserAccountDO::getPhone, phone));
  }

  /**
   * 禁用与注销要明确告知原因：用户需要知道该走什么流程，
   * 而这两个状态本身不构成可被批量探测的信息（都得先知道密码或验证码）。
   */
  private void ensureAccountUsable(UserAccountDO account) {
    if (account.getStatus() == UserConst.STATUS_DISABLED) {
      throw new BizException(ErrorCodeEnum.ACCOUNT_FROZEN);
    }
    if (account.getStatus() == UserConst.STATUS_CANCELLED) {
      throw new BizException(ErrorCodeEnum.ACCOUNT_CANCELLED);
    }
  }

  private String loadNickname(Long userId) {
    UserProfileDO profile =
      userProfileMapper.selectOne(new LambdaQueryWrapper<UserProfileDO>().eq(UserProfileDO::getUserId, userId));
    return profile == null ? "" : profile.getNickname();
  }

  private void updateLastLoginTime(Long userId) {
    UserAccountDO update = new UserAccountDO();
    update.setId(userId);
    update.setLastLoginTime(LocalDateTime.now());
    userAccountMapper.updateById(update);
  }

  /**
   * 更新时间信息；设备首次出现时补插一行。
   *
   * <p>并发登录同一台新设备可能同时插入，由 uk_user_device 拦下后者，
   * 这里吞掉异常即可 —— 两行的业务含义完全相同。
   */
  private void touchDevice(Long userId, String deviceId, Integer deviceType, String deviceName) {
    UserDeviceDO existing =
      userDeviceMapper.selectOne(new LambdaQueryWrapper<UserDeviceDO>().eq(UserDeviceDO::getUserId, userId)
        .eq(UserDeviceDO::getDeviceId, deviceId));
    if (existing != null) {
      UserDeviceDO update = new UserDeviceDO();
      update.setId(existing.getId());
      update.setLastActiveTime(LocalDateTime.now());
      update.setDeviceType(deviceType);
      update.setDeviceName(deviceName);
      userDeviceMapper.updateById(update);
      return;
    }

    UserDeviceDO device = new UserDeviceDO();
    device.setUserId(userId);
    device.setDeviceId(deviceId);
    device.setDeviceType(deviceType);
    device.setDeviceName(deviceName);
    device.setLastActiveTime(LocalDateTime.now());
    try {
      userDeviceMapper.insert(device);
    } catch (DuplicateKeyException e) {
      log.debug("并发登录插入设备撞唯一键，忽略 userId={} deviceId={}", userId, deviceId);
    }
  }

  private String resolveCountryCode(String countryCode) {
    return countryCode == null ? UserConst.DEFAULT_COUNTRY_CODE : countryCode;
  }

}

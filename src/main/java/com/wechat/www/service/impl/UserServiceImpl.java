package com.wechat.www.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.wechat.www.common.constant.UserConst;
import com.wechat.www.common.exception.BizException;
import com.wechat.www.common.exception.ErrorCodeEnum;
import com.wechat.www.common.util.PhoneUtils;
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
import com.wechat.www.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.regex.Pattern;

/**
 * 用户服务实现。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

  /** 密码允许的字符集：ASCII 可打印字符（0x21~0x7E），不含空格 */
  private static final Pattern PASSWORD_ALLOWED = Pattern.compile("^[!-~]+$");

  /** 字母 */
  private static final Pattern PASSWORD_LETTER = Pattern.compile("[A-Za-z]");

  /** 数字 */
  private static final Pattern PASSWORD_DIGIT = Pattern.compile("\\d");

  /** 符号：非字母非数字的 ASCII 字符 */
  private static final Pattern PASSWORD_SYMBOL = Pattern.compile("[^A-Za-z0-9]");

  private final UserAccountMapper userAccountMapper;
  private final UserProfileMapper userProfileMapper;
  private final UserDeviceMapper userDeviceMapper;
  private final SmsService smsService;
  private final PasswordEncoder passwordEncoder;

  @Override
  @Transactional(rollbackFor = Exception.class)
  public RegisterUserVO register(RegisterUserDTO dto) {
    String phone = dto.getPhone();
    // 密码策略先于验证码：密码不合法时不消耗用户刚收到的验证码
    checkPasswordPolicy(dto.getPassword());

    // 验证码先于查重：未持有验证码的人无法借此探测手机号是否已注册
    VerifySmsCodeDTO verifyDto = new VerifySmsCodeDTO();
    verifyDto.setPhone(phone);
    verifyDto.setScene(SmsSceneEnum.REGISTER.getCode());
    verifyDto.setCode(dto.getCode());
    smsService.verifyCode(verifyDto);

    String countryCode = dto.getCountryCode() == null ? UserConst.DEFAULT_COUNTRY_CODE : dto.getCountryCode();
    ensurePhoneNotRegistered(countryCode, phone);

    UserAccountDO account = new UserAccountDO();
    account.setCountryCode(countryCode);
    account.setPhone(phone);
    account.setPasswordHash(passwordEncoder.encode(dto.getPassword()));
    try {
      userAccountMapper.insert(account);
    } catch (DuplicateKeyException e) {
      // 并发注册兜底：两个请求同时通过上面的查重时，由唯一键 uk_country_phone 拦下后一个
      throw new BizException(ErrorCodeEnum.PHONE_ALREADY_REGISTERED, "该手机号已注册");
    }
    Long userId = account.getId();

    UserProfileDO profile = new UserProfileDO();
    profile.setUserId(userId);
    profile.setNickname(dto.getNickname());
    userProfileMapper.insert(profile);

    UserDeviceDO device = new UserDeviceDO();
    device.setUserId(userId);
    device.setDeviceId(dto.getDeviceId());
    userDeviceMapper.insert(device);

    log.info("用户注册成功 userId={} phone={}", userId, PhoneUtils.mask(phone));
    return new RegisterUserVO(userId, dto.getNickname());
  }

  /**
   * 密码策略（对标微信注册规则）：8~16 位，字母 / 数字 / 符号至少包含两类。
   *
   * <p>只允许 ASCII 可打印字符，避免中文、空格、控制字符进入密码，
   * 也让"字母/数字/符号"的分类没有歧义。
   */
  private void checkPasswordPolicy(String password) {
    if (password.length() < UserConst.PASSWORD_MIN_LENGTH || password.length() > UserConst.PASSWORD_MAX_LENGTH) {
      throw new BizException(ErrorCodeEnum.PASSWORD_LENGTH_INVALID, "密码长度需为 8~16 位");
    }
    if (!PASSWORD_ALLOWED.matcher(password).matches()) {
      throw new BizException(ErrorCodeEnum.PASSWORD_STRENGTH_WEAK, "密码只能包含字母、数字和符号");
    }
    int charTypes = 0;
    if (PASSWORD_LETTER.matcher(password).find()) {
      charTypes++;
    }
    if (PASSWORD_DIGIT.matcher(password).find()) {
      charTypes++;
    }
    if (PASSWORD_SYMBOL.matcher(password).find()) {
      charTypes++;
    }
    if (charTypes < UserConst.PASSWORD_MIN_CHAR_TYPES) {
      throw new BizException(ErrorCodeEnum.PASSWORD_STRENGTH_WEAK, "密码需包含字母、数字、符号中的至少两种");
    }
  }

  /**
   * 手机号查重：走唯一键 uk_country_phone 的等值查询，先给友好提示，并发由唯一键兜底。
   */
  private void ensurePhoneNotRegistered(String countryCode, String phone) {
    Long count = userAccountMapper.selectCount(new LambdaQueryWrapper<UserAccountDO>()
      .eq(UserAccountDO::getCountryCode, countryCode)
      .eq(UserAccountDO::getPhone, phone));
    if (count > 0) {
      throw new BizException(ErrorCodeEnum.PHONE_ALREADY_REGISTERED, "该手机号已注册");
    }
  }

}

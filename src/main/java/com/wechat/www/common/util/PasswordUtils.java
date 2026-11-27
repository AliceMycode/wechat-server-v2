package com.wechat.www.common.util;

import com.wechat.www.common.constant.PasswordConst;
import com.wechat.www.common.exception.BizException;
import com.wechat.www.common.exception.ErrorCodeEnum;

import java.util.regex.Pattern;

/**
 * 密码策略（对标微信注册规则）：8~16 位，字母 / 数字 / 符号至少包含两类。
 *
 * <p>只允许 ASCII 可打印字符，避免中文、空格、控制字符进入密码，
 * 也让"字母/数字/符号"的分类没有歧义。
 *
 * <p>注册、修改密码、重置密码共用同一套规则，因此从 Service 层抽出来，
 * 各业务类只需调用 {@link #checkPolicy(String)}。
 */
public final class PasswordUtils {

  /**
   * 允许的字符集：ASCII 可打印字符（0x21~0x7E），不含空格
   */
  private static final Pattern ALLOWED = Pattern.compile("^[!-~]+$");

  /**
   * 字母
   */
  private static final Pattern LETTER = Pattern.compile("[A-Za-z]");

  /**
   * 数字
   */
  private static final Pattern DIGIT = Pattern.compile("\\d");

  /**
   * 符号：非字母非数字的 ASCII 字符
   */
  private static final Pattern SYMBOL = Pattern.compile("[^A-Za-z0-9]");

  private PasswordUtils() {
  }

  /**
   * 校验密码是否符合策略，不符合直接抛业务异常。
   *
   * @param password 明文密码
   */
  public static void checkPolicy(String password) {
    if (password.length() < PasswordConst.PASSWORD_MIN_LENGTH || password.length() > PasswordConst.PASSWORD_MAX_LENGTH) {
      throw new BizException(ErrorCodeEnum.PASSWORD_LENGTH_INVALID);
    }
    if (!ALLOWED.matcher(password).matches()) {
      throw new BizException(ErrorCodeEnum.PASSWORD_STRENGTH_WEAK);
    }
    int charTypes = 0;
    if (LETTER.matcher(password).find()) {
      charTypes++;
    }
    if (DIGIT.matcher(password).find()) {
      charTypes++;
    }
    if (SYMBOL.matcher(password).find()) {
      charTypes++;
    }
    if (charTypes < PasswordConst.PASSWORD_MIN_CHAR_TYPES) {
      throw new BizException(ErrorCodeEnum.PASSWORD_STRENGTH_WEAK);
    }
  }

}

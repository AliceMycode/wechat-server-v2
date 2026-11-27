package com.wechat.www.common.constant;

/**
 * 密码策略常量。注册、修改密码、重置密码共用同一套规则。
 */
public final class PasswordConst {

  /**
   * 密码长度下限（对标微信注册规则：8~16 位）
   */
  public static final int PASSWORD_MIN_LENGTH = 8;

  /**
   * 密码长度上限
   */
  public static final int PASSWORD_MAX_LENGTH = 16;

  /**
   * 密码至少包含的字符类别数：字母 / 数字 / 符号 三类中至少两类
   */
  public static final int PASSWORD_MIN_CHAR_TYPES = 2;

  private PasswordConst() {
  }

}

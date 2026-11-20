package com.wechat.www.common.constant;

/**
 * 用户业务常量。密码策略、默认区号等约定值统一收敛在此，
 * 禁止散落在各业务类中（规约【强制】：不允许魔法值直接出现在代码里）。
 */
public final class UserConst {

  /** 注册默认国家区号：客户端未传时使用（当前仅支持中国大陆） */
  public static final String DEFAULT_COUNTRY_CODE = "86";

  /** 密码长度下限（对标微信注册规则：8~16 位） */
  public static final int PASSWORD_MIN_LENGTH = 8;

  /** 密码长度上限 */
  public static final int PASSWORD_MAX_LENGTH = 16;

  /** 密码至少包含的字符类别数：字母 / 数字 / 符号 三类中至少两类 */
  public static final int PASSWORD_MIN_CHAR_TYPES = 2;

  /** user_account.status：正常 */
  public static final int STATUS_NORMAL = 0;

  /** user_account.status：禁用 */
  public static final int STATUS_DISABLED = 1;

  /** user_account.status：注销 */
  public static final int STATUS_CANCELLED = 2;

  private UserConst() {
  }

}

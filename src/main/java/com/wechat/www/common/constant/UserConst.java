package com.wechat.www.common.constant;

/**
 * 用户账号与资料常量。密码策略见 {@link PasswordConst}，设备见 {@link DeviceConst}，
 * 手机号见 {@link PhoneConst}。
 *
 * <p>规约【强制】：不允许魔法值直接出现在代码里。
 */
public final class UserConst {

  /**
   * 昵称最大长度
   */
  public static final int NICKNAME_MAX_LENGTH = 20;

  /**
   * user_account.status：正常
   */
  public static final int STATUS_NORMAL = 0;

  /**
   * user_account.status：禁用
   */
  public static final int STATUS_DISABLED = 1;

  /**
   * user_account.status：注销
   */
  public static final int STATUS_CANCELLED = 2;

  private UserConst() {
  }

}

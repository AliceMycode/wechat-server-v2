package com.wechat.www.common.constant;

/**
 * 手机号与区号常量。当前仅支持中国大陆，放开多地区时只需扩展此类。
 */
public final class PhoneConst {

  /**
   * 注册默认国家区号：客户端未传时使用（当前仅支持中国大陆）
   */
  public static final String DEFAULT_COUNTRY_CODE = "86";

  /**
   * 国家区号校验正则：当前仅支持中国大陆
   */
  public static final String COUNTRY_CODE_REGEXP = "^" + DEFAULT_COUNTRY_CODE + "$";

  /**
   * 大陆手机号校验正则
   */
  public static final String PHONE_REGEXP = "^1[3-9]\\d{9}$";

  private PhoneConst() {
  }

}

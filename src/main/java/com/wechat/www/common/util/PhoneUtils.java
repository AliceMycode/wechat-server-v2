package com.wechat.www.common.util;

/**
 * 手机号工具。
 */
public final class PhoneUtils {

  private PhoneUtils() {
  }

  /**
   * 日志脱敏：138****8000（安全规约：日志中的用户敏感信息需脱敏）。
   */
  public static String mask(String phone) {
    return phone.substring(0, 3) + "****" + phone.substring(phone.length() - 4);
  }

}

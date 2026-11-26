package com.wechat.www.common.constant;

/**
 * 短信业务常量。业务约定的取值（状态、次数阈值等）统一收敛在此，
 * 禁止散落在各业务类中（规约【强制】：不允许魔法值直接出现在代码里）。
 */
public final class SmsConst {

  /**
   * sms_record.send_status：发送成功
   */
  public static final int SEND_STATUS_SUCCESS = 1;

  /**
   * sms_record.send_status：发送失败
   */
  public static final int SEND_STATUS_FAIL = 2;

  /**
   * 同一验证码允许的最大核验失败次数，达到后拒绝核验，直到用户重新获取验证码
   */
  public static final int VERIFY_MAX_FAIL_COUNT = 5;

  /**
   * 每分钟秒数：验证码有效期按秒配置，渠道模板的分钟参数由它换算
   */
  public static final int SECONDS_PER_MINUTE = 60;

  private SmsConst() {
  }

}

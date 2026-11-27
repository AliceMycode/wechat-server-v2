package com.wechat.www.domain.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 推送通道（对应 user_device.push_provider 字段，取值与表注释一致）。
 *
 * <p>0 表示无推送通道（Web 端、未接入推送的客户端均为此值），不建枚举项。
 */
@Getter
@AllArgsConstructor
public enum PushProviderEnum {

  /**
   * 苹果 APNs（iOS / macOS）
   */
  APNS(1, "APNs"),

  /**
   * 华为推送
   */
  HUAWEI(2, "华为"),

  /**
   * 小米推送
   */
  XIAOMI(3, "小米"),

  /**
   * OPPO 推送
   */
  OPPO(4, "OPPO"),

  /**
   * vivo 推送
   */
  VIVO(5, "vivo"),

  /**
   * 荣耀推送
   */
  HONOR(6, "荣耀"),

  /**
   * 阿里云移动推送 EMAS
   */
  EMAS(7, "EMAS");

  /**
   * 推送通道码，与数据库 user_device.push_provider 取值一致
   */
  private final Integer code;

  /**
   * 推送通道中文名
   */
  private final String desc;

}

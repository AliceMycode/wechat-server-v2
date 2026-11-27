package com.wechat.www.domain.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 用户设备类型（对应 user_device.device_type 字段，取值与表注释一致）。
 *
 * <p>0 表示客户端未上报或无法识别，不代表任何一种具体终端，故不建枚举项；
 * 取值范围的校验仍由 {@link com.wechat.www.common.constant.DeviceConst} 的上下界负责。
 */
@Getter
@AllArgsConstructor
public enum DeviceTypeEnum {

  /**
   * Electron 桌面客户端
   */
  ELECTRON(1, "Electron"),

  /**
   * iOS 客户端
   */
  IOS(2, "iOS"),

  /**
   * Android 客户端
   */
  ANDROID(3, "Android"),

  /**
   * Web 浏览器
   */
  WEB(4, "Web");

  /**
   * 设备类型码，与数据库 user_device.device_type 取值一致
   */
  private final Integer code;

  /**
   * 设备类型中文名
   */
  private final String desc;

}

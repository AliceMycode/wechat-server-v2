package com.wechat.www.common.constant;

/**
 * 设备标识与设备信息相关常量。设备类型的取值字典见
 * {@link com.wechat.www.domain.enums.DeviceTypeEnum}。
 */
public final class DeviceConst {

  /**
   * 设备标识最大长度：客户端生成的 UUID 为 36 位，留余量兼容其他形态
   */
  public static final int DEVICE_ID_MAX_LENGTH = 64;

  /**
   * 设备名称最大长度
   */
  public static final int DEVICE_NAME_MAX_LENGTH = 64;

  /**
   * 设备类型取值下界：0 表示未识别
   */
  public static final int DEVICE_TYPE_MIN = 0;

  /**
   * 设备类型取值上界：4-Web 为当前最大取值
   */
  public static final int DEVICE_TYPE_MAX = 4;

  private DeviceConst() {
  }

}

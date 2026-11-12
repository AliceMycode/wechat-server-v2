package com.wechat.www.domain.enums;

import com.wechat.www.common.exception.BizException;
import com.wechat.www.common.exception.ErrorCode;
import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 短信验证码业务场景（对应 sms_record.scene 字段，取值与表注释一致）。
 */
@Getter
@AllArgsConstructor
public enum SmsSceneEnum {

  /** 注册 */
  REGISTER("register", "注册"),

  /** 登录 */
  LOGIN("login", "登录"),

  /** 重置密码 */
  RESET("reset", "重置密码");

  /** 场景标识，与数据库 sms_record.scene 取值一致 */
  private final String code;

  /** 场景中文名 */
  private final String desc;

  /**
   * 按场景标识解析枚举，非法值直接拒绝，不让脏值往下走。
   */
  public static SmsSceneEnum of(String code) {
    for (SmsSceneEnum scene : values()) {
      if (scene.code.equals(code)) {
        return scene;
      }
    }
    throw new BizException(ErrorCode.SCENE_INVALID, "不支持的场景类型");
  }

}

package com.wechat.www.common.exception;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 全局错误码枚举。
 *
 * <p>按《阿里巴巴 Java 开发手册》规范：错误码为 5 位字符串 = 错误来源 + 四位编号。
 * A=用户端错误，B=系统执行出错，C=调用第三方服务出错；编号取自手册附表 3 的既有语义。
 */
@Getter
@AllArgsConstructor
public enum ErrorCode {

  /** 一切正常 */
  SUCCESS("00000", "成功"),

  /** 用户请求参数错误（一级宏观错误码） */
  PARAM_INVALID("A0400", "请求参数错误"),

  /** 手机格式校验失败 */
  PHONE_FORMAT_ERROR("A0151", "手机号格式不正确"),

  /** 参数格式不匹配（如短信场景值非法） */
  SCENE_INVALID("A0421", "参数格式不匹配"),

  /** 请求 JSON 解析失败 */
  JSON_PARSE_ERROR("A0427", "请求体解析失败"),

  /** 请求次数超出限制（短信频控） */
  SMS_SEND_FREQUENT("A0501", "请求次数超出限制"),

  /** 短信提醒服务失败（第三方渠道异常） */
  SMS_SEND_FAILED("C0501", "短信发送失败，请稍后再试"),

  /** 系统执行出错（一级宏观错误码） */
  SYSTEM_ERROR("B0001", "系统繁忙，请稍后再试");

  /** 五位错误码：来源字母 + 四位数字 */
  private final String code;

  /** 默认提示信息 */
  private final String message;

}

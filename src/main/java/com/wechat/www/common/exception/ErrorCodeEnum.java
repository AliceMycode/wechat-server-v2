package com.wechat.www.common.exception;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 全局错误码枚举。
 *
 * <p>按《阿里巴巴 Java 开发手册》规范：错误码为 5 位字符串 = 错误来源 + 四位编号。
 * A=用户端错误，B=系统执行出错，C=调用第三方服务出错；编号取自手册附表 3 的既有语义。
 *
 * <p>本枚举<b>只装业务码</b>，不关心 HTTP 状态码 —— 401/400/500 属于传输层协议，
 * 由 {@link GlobalExceptionHandler} 统一决定。
 *
 * <p>成功码 {@code 00000} 也不在这里：它是"一切 ok"的响应码而非错误码，
 * 由 {@link com.wechat.www.common.result.Result} 内部持有。
 */
@Getter
@AllArgsConstructor
public enum ErrorCodeEnum {

  /**
   * 用户请求参数错误（一级宏观错误码）
   */
  PARAM_INVALID("A0400", "请求参数错误"),

  /**
   * 手机号已注册（手册附表 3：A0111 用户名已存在）
   */
  PHONE_ALREADY_REGISTERED("A0111", "该手机号已注册"),

  /**
   * 密码长度不够（手册附表 3：A0121）
   */
  PASSWORD_LENGTH_INVALID("A0121", "密码长度需为 8~16 位"),

  /**
   * 密码强度不够（手册附表 3：A0122）
   */
  PASSWORD_STRENGTH_WEAK("A0122", "密码需包含字母、数字、符号中的至少两种"),

  /**
   * 手机格式校验失败
   */
  PHONE_FORMAT_ERROR("A0151", "手机号格式不正确"),

  /**
   * 用户账户不存在（手册附表 3：A0201）。仅用于短信登录 ——
   * 此时号码归属已被验证码证明，不存在账号枚举风险
   */
  ACCOUNT_NOT_FOUND("A0201", "该手机号未注册"),

  /**
   * 用户账户被冻结（手册附表 3：A0202）
   */
  ACCOUNT_FROZEN("A0202", "账户已被冻结"),

  /**
   * 用户账户已作废（手册附表 3：A0203）
   */
  ACCOUNT_CANCELLED("A0203", "账户已作废"),

  /**
   * 用户密码错误（手册附表 3：A0210）。密码登录时"账号不存在"与"密码错误"
   * 统一返回此码，避免攻击者用登录接口批量探测手机号是否已注册
   */
  LOGIN_FAILED("A0210", "手机号或密码错误"),

  /**
   * 用户输入密码错误次数超限（手册附表 3：A0211）
   */
  PASSWORD_FAIL_LIMIT("A0211", "密码错误次数过多，请稍后再试"),

  /**
   * 用户登录已过期（手册附表 3：A0230）。与 {@link #UNAUTHORIZED} 同为认证类错误，
   * 由 GlobalExceptionHandler 统一映射为 HTTP 401
   */
  LOGIN_EXPIRED("A0230", "登录已过期，请重新登录"),

  /**
   * 访问未授权（手册附表 3：A0301）
   */
  UNAUTHORIZED("A0301", "访问未授权，请先登录"),

  /**
   * 参数格式不匹配（如短信场景值非法）
   */
  SCENE_INVALID("A0421", "参数格式不匹配"),

  /**
   * 请求 JSON 解析失败
   */
  JSON_PARSE_ERROR("A0427", "请求体解析失败"),

  /**
   * 请求次数超出限制（短信频控）
   */
  SMS_SEND_FREQUENT("A0501", "请求次数超出限制"),

  /**
   * 短信提醒服务失败（第三方渠道异常）
   */
  SMS_SEND_FAILED("C0501", "短信发送失败，请稍后再试"),

  /**
   * 用户验证码错误（手册附表 3：A0240）
   */
  SMS_CODE_ERROR("A0240", "验证码错误或已过期"),

  /**
   * 用户验证码尝试次数超限（手册附表 3：A0241）
   */
  SMS_CODE_ATTEMPT_LIMIT("A0241", "验证码错误次数过多，请重新获取"),

  /**
   * 系统执行出错（一级宏观错误码）
   */
  SYSTEM_ERROR("B0001", "系统繁忙，请稍后再试");

  /**
   * 五位错误码：来源字母 + 四位数字
   */
  private final String code;

  /**
   * 默认提示信息
   */
  private final String message;

}

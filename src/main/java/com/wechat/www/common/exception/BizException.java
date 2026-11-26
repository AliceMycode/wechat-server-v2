package com.wechat.www.common.exception;

import lombok.Getter;

/**
 * 业务异常：Service/Manager 层校验不通过或渠道调用失败时抛出，
 * 由 GlobalExceptionHandler 统一转换为 Result 响应，Controller 层不写 try-catch。
 *
 * <p>{@link #getMessage()} 就是返回给前端的展示文案：
 * 默认取错误码的 message，构造时传了自定义文案则以自定义为准。
 * 错误码本身用 {@link #getErrorCode()} 取，两者不要再混用。
 */
@Getter
public class BizException extends RuntimeException {

  /**
   * 错误码（决定业务码与 HTTP 状态码）
   */
  private final ErrorCodeEnum errorCode;

  public BizException(ErrorCodeEnum errorCode) {
    super(errorCode.getMessage());
    this.errorCode = errorCode;
  }

  /**
   * @param message 覆盖默认提示，给用户更具体的文案
   */
  public BizException(ErrorCodeEnum errorCode, String message) {
    super(message);
    this.errorCode = errorCode;
  }

}

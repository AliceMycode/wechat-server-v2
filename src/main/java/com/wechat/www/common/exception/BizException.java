package com.wechat.www.common.exception;

import lombok.Getter;

/**
 * 业务异常：Service/Manager 层校验不通过或渠道调用失败时抛出，
 * 由 GlobalExceptionHandler 统一转换为 Result 响应，Controller 层不写 try-catch。
 */
@Getter
public class BizException extends RuntimeException {

  /**
   * 错误码
   */
  private final ErrorCode errorCode;

  public BizException(ErrorCode errorCode) {
    super(errorCode.getMessage());
    this.errorCode = errorCode;
  }

  /**
   * @param message 覆盖默认提示，给用户更具体的文案
   */
  public BizException(ErrorCode errorCode, String message) {
    super(message);
    this.errorCode = errorCode;
  }

}

package com.wechat.www.common.result;

import com.wechat.www.common.exception.ErrorCodeEnum;
import lombok.AllArgsConstructor;
import lombok.Data;

/**
 * 统一响应体：所有 HTTP 接口的返回包装。
 *
 * <p>成功码 {@code 00000} 是阿里规范里"一切 ok"的返回码，
 * 它是响应码而非错误码，所以定义在本类而不放进 {@link ErrorCodeEnum}。
 */
@Data
@AllArgsConstructor
public class Result<T> {

  /**
   * 成功码：一切正常
   */
  private static final String SUCCESS_CODE = "00000";

  /**
   * 成功提示
   */
  private static final String SUCCESS_MESSAGE = "成功";

  /**
   * 五位业务码：00000 表示成功，其余为错误码
   */
  private String code;

  /**
   * 提示信息
   */
  private String message;

  /**
   * 业务数据，失败时为 null（Jackson 已配置 non_null，不输出该字段）
   */
  private T data;

  public static <T> Result<T> ok() {
    return new Result<>(SUCCESS_CODE, SUCCESS_MESSAGE, null);
  }

  public static <T> Result<T> ok(T data) {
    return new Result<>(SUCCESS_CODE, SUCCESS_MESSAGE, data);
  }

  /**
   * 失败响应，使用错误码的默认提示。
   */
  public static <T> Result<T> fail(ErrorCodeEnum errorCode) {
    return new Result<>(errorCode.getCode(), errorCode.getMessage(), null);
  }

  /**
   * 失败响应，用更具体的文案覆盖默认提示。
   */
  public static <T> Result<T> fail(ErrorCodeEnum errorCode, String message) {
    return new Result<>(errorCode.getCode(), message, null);
  }

}

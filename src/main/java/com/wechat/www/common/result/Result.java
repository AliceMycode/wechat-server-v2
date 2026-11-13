package com.wechat.www.common.result;

import com.wechat.www.common.exception.ErrorCode;
import lombok.AllArgsConstructor;
import lombok.Data;

/**
 * 统一响应体：所有 HTTP 接口的返回包装。
 * code 为 "00000" 表示成功，其余为错误码（阿里规范 A/B/C 五位错误码）。
 */
@Data
@AllArgsConstructor
public class Result<T> {

  /**
   * 五位错误码，00000 表示成功
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
    return new Result<>(ErrorCode.SUCCESS.getCode(), ErrorCode.SUCCESS.getMessage(), null);
  }

  public static <T> Result<T> ok(T data) {
    return new Result<>(ErrorCode.SUCCESS.getCode(), ErrorCode.SUCCESS.getMessage(), data);
  }

  public static <T> Result<T> fail(ErrorCode errorCode, String message) {
    return new Result<>(errorCode.getCode(), message, null);
  }

}

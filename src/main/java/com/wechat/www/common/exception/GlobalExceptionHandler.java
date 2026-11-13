package com.wechat.www.common.exception;

import com.wechat.www.common.result.Result;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.resource.NoResourceFoundException;

/**
 * 全局异常处理：把异常统一转换为 Result 响应。
 *
 * <p>HTTP 状态码与业务码分层：参数问题 400、系统异常 500，
 * 业务失败（BizException）返回 200，前端按 code 判断。
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

  /**
   * 业务异常：可预期的失败（频控、场景非法等），warn 级别即可，别用 error 触发报警
   */
  @ExceptionHandler(BizException.class)
  public ResponseEntity<Result<Void>> handleBizException(BizException e) {
    log.warn("业务异常 code={} message={}", e
      .getErrorCode()
      .getCode(), e.getMessage());
    return ResponseEntity.ok(Result.fail(e.getErrorCode(), e.getMessage()));
  }

  /**
   * @Valid 校验失败：取第一条错误提示返回给前端
   */
  @ExceptionHandler(MethodArgumentNotValidException.class)
  public ResponseEntity<Result<Void>> handleValidException(MethodArgumentNotValidException e) {
    FieldError fieldError = e
      .getBindingResult()
      .getFieldError();
    String message = fieldError == null ? ErrorCode.PARAM_INVALID.getMessage() : fieldError.getDefaultMessage();
    return ResponseEntity
      .badRequest()
      .body(Result.fail(ErrorCode.PARAM_INVALID, message));
  }

  /**
   * 请求体不是合法 JSON
   */
  @ExceptionHandler(HttpMessageNotReadableException.class)
  public ResponseEntity<Result<Void>> handleNotReadable(HttpMessageNotReadableException e) {
    return ResponseEntity
      .badRequest()
      .body(Result.fail(ErrorCode.JSON_PARSE_ERROR, ErrorCode.JSON_PARSE_ERROR.getMessage()));
  }

  /**
   * 静态资源不存在（如浏览器自动请求 /favicon.ico）：静默返回 404，
   * 不走兜底 Exception 处理，避免每次都打一大段无意义堆栈
   */
  @ExceptionHandler(NoResourceFoundException.class)
  public ResponseEntity<Void> handleNoResourceFound(NoResourceFoundException e) {
    return ResponseEntity
      .notFound()
      .build();
  }

  /**
   * 兜底：未预期异常，error 级别记录堆栈（保留案发现场），对外只给笼统提示不泄露细节
   */
  @ExceptionHandler(Exception.class)
  public ResponseEntity<Result<Void>> handleException(Exception e) {
    log.error("系统异常", e);
    return ResponseEntity
      .internalServerError()
      .body(Result.fail(ErrorCode.SYSTEM_ERROR, ErrorCode.SYSTEM_ERROR.getMessage()));
  }

}

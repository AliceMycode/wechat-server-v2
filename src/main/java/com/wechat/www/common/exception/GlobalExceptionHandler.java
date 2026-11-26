package com.wechat.www.common.exception;

import com.wechat.www.common.result.Result;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.resource.NoResourceFoundException;

/**
 * 全局异常处理：把异常统一转换为 Result 响应。
 * 业务码和 HTTP 状态码本来就是两个维度：业务码在 body（Result.code），HTTP 状态码在响应行。
 * 所以准确的结论是：没有 ResponseEntity 不是"区分不了两种码"，而是 HTTP 状态码只能是 200。
 *
 * <p>两层码各司其职，不混在一起：
 * <ul>
 *   <li><b>HTTP 状态码</b>（传输层协议）：认证 401、参数 400、系统 500，在这里决定</li>
 *   <li><b>业务码</b>（Result.code）：A/B/C 五位码，由 ErrorCodeEnum 提供，前端按它给提示</li>
 * </ul>
 * 业务失败（密码错、频控、验证码错等）仍返回 HTTP 200，前端只看 code。
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

  /**
   * 业务异常：可预期的失败（频控、场景非法等），warn 级别即可，别用 error 触发报警。
   *
   * <p>唯一需要提升到 401 的是认证类错误 —— 让前端拦截器只需判断
   * HTTP 401 就能统一跳登录页，不必再去对比 A0230 / A0301 这类业务码。
   */
  @ExceptionHandler(BizException.class)
  public ResponseEntity<Result<Void>> handleBizException(BizException e) {
    ErrorCodeEnum errorCode = e.getErrorCode();
    log.warn("业务异常 code={} message={}", errorCode.getCode(), e.getMessage());

    boolean authError = errorCode == ErrorCodeEnum.LOGIN_EXPIRED || errorCode == ErrorCodeEnum.UNAUTHORIZED;
    ResponseEntity.BodyBuilder builder =
      authError ? ResponseEntity.status(HttpStatus.UNAUTHORIZED) : ResponseEntity.ok();
    return builder.body(Result.fail(errorCode, e.getMessage()));
  }

  /**
   * @Valid 校验失败：取第一条错误提示返回给前端
   */
  @ExceptionHandler(MethodArgumentNotValidException.class)
  public ResponseEntity<Result<Void>> handleValidException(MethodArgumentNotValidException e) {
    ErrorCodeEnum errorCode = ErrorCodeEnum.PARAM_INVALID;
    FieldError fieldError = e.getBindingResult().getFieldError();
    String message = fieldError == null ? errorCode.getMessage() : fieldError.getDefaultMessage();
    return ResponseEntity.badRequest().body(Result.fail(errorCode, message));
  }

  /**
   * 请求体不是合法 JSON
   */
  @ExceptionHandler(HttpMessageNotReadableException.class)
  public ResponseEntity<Result<Void>> handleNotReadable(HttpMessageNotReadableException e) {
    return ResponseEntity.badRequest().body(Result.fail(ErrorCodeEnum.JSON_PARSE_ERROR));
  }

  /**
   * 静态资源不存在（如浏览器自动请求 /favicon.ico）：静默返回 404，
   * 不走兜底 Exception 处理，避免每次都打一大段无意义堆栈
   */
  @ExceptionHandler(NoResourceFoundException.class)
  public ResponseEntity<Void> handleNoResourceFound(NoResourceFoundException e) {
    return ResponseEntity.notFound().build();
  }

  /**
   * 兜底：未预期异常，error 级别记录堆栈（保留案发现场），对外只给笼统提示不泄露细节
   */
  @ExceptionHandler(Exception.class)
  public ResponseEntity<Result<Void>> handleException(Exception e) {
    log.error("系统异常", e);
    return ResponseEntity.internalServerError().body(Result.fail(ErrorCodeEnum.SYSTEM_ERROR));
  }

}

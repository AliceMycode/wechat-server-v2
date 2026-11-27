package com.wechat.www.common.interceptor;

import com.wechat.www.common.annotation.IpRateLimit;
import com.wechat.www.common.constant.RedisKeyConst;
import com.wechat.www.common.exception.BizException;
import com.wechat.www.common.exception.ErrorCodeEnum;
import com.wechat.www.common.util.IpUtils;
import com.wechat.www.manager.RedisRateLimiter;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

import java.time.Duration;

/**
 * IP 维度限流拦截器：读取 Controller 方法上的 {@link IpRateLimit}，进入业务前做固定窗口计数。
 *
 * <p>选 Interceptor 而不是 Filter：Filter 跑在 DispatcherServlet 之外，抛出的异常不经过
 * {@code @RestControllerAdvice}，得自己手写 JSON 响应；Interceptor 的异常能被
 * {@link com.wechat.www.common.exception.GlobalExceptionHandler} 正常接住。
 */
@Component
@RequiredArgsConstructor
public class IpRateLimitInterceptor implements HandlerInterceptor {

  private final RedisRateLimiter redisRateLimiter;

  @Override
  public boolean preHandle(@NonNull HttpServletRequest request, @NonNull HttpServletResponse response,
    @NonNull Object handler) {
    // 静态资源等非 Controller 方法（如 ResourceHttpRequestHandler）直接放行
    if (!(handler instanceof HandlerMethod handlerMethod)) {
      return true;
    }
    IpRateLimit rule = handlerMethod.getMethodAnnotation(IpRateLimit.class);
    if (rule == null) {
      return true;
    }
    // key 带"类名.方法名"：不同接口各自独立配额，不互相消耗
    String methodKey =
      handlerMethod.getBeanType().getSimpleName() + "." + handlerMethod.getMethod().getName();
    String key = RedisKeyConst.RATE_LIMIT_IP_PREFIX + methodKey + ":" + IpUtils.resolveClientIp(request);
    if (redisRateLimiter.hit(key + ":minute", Duration.ofMinutes(1)) > rule.maxPerMinute()
      || redisRateLimiter.hit(key + ":daily", Duration.ofDays(1)) > rule.maxPerDay()) {
      throw new BizException(ErrorCodeEnum.REQUEST_FREQUENT);
    }
    return true;
  }

}

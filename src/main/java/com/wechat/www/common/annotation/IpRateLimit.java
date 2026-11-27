package com.wechat.www.common.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * IP 维度限流注解：标在 Controller 方法上，由 {@link com.wechat.www.common.interceptor.IpRateLimitInterceptor} 在进入业务前拦截。
 *
 * <p>两个窗口都以"请求次数"计数，不区分成功与失败 —— 拦截器看不到业务结果，
 * 这是接入层限流的固有限制，也是 Nginx、网关限流的通行做法。
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface IpRateLimit {

  /**
   * 同一 IP 每分钟最多请求次数
   */
  int minute();

  /**
   * 同一 IP 每 24 小时（滚动窗口）最多请求次数
   */
  int daily();

}

package com.wechat.www.common.util;

import jakarta.servlet.http.HttpServletRequest;

/**
 * 客户端 IP 提取。
 *
 * <p>取 {@code X-Forwarded-For} 首段，前提是后端不直接暴露公网、由 Nginx 等代理覆写该头；
 * 若后端端口直接对外，攻击者可自行伪造该头绕过所有 IP 维度限流。
 */
public final class IpUtils {

  private static final String X_FORWARDED_FOR = "X-Forwarded-For";

  private IpUtils() {
  }

  /**
   * 取客户端真实 IP：优先 X-Forwarded-For 首段（多级代理格式为 "client, proxy1, proxy2"），
   * 未经过代理时取直连地址。
   */
  public static String resolveClientIp(HttpServletRequest request) {
    String xff = request.getHeader(X_FORWARDED_FOR);
    if (xff != null && !xff.isEmpty()) {
      return xff.split(",")[0].trim();
    }
    return request.getRemoteAddr();
  }

}

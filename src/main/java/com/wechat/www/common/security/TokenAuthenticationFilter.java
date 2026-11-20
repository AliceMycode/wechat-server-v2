package com.wechat.www.common.security;

import com.wechat.www.manager.TokenManager;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.NonNull;
import org.springframework.http.HttpHeaders;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.security.web.authentication.preauth.PreAuthenticatedAuthenticationToken;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

/**
 * Token 认证过滤器：从 {@code Authorization: Bearer <token>} 解析会话，
 * 命中后把 {@link LoginUser} 写入 SecurityContext。
 *
 * <p>拿不到 token 或 token 无效时<b>不抛异常</b>，直接放行：
 * 是否需要登录由 {@code authorizeHttpRequests} 的判断决定，未认证的请求
 * 最终由 {@code AuthenticationEntryPoint} 统一输出 401。
 */
@RequiredArgsConstructor
public class TokenAuthenticationFilter extends OncePerRequestFilter {

  private static final String BEARER_PREFIX = "Bearer ";

  private final TokenManager tokenManager;

  @Override
  protected void doFilterInternal(@NonNull HttpServletRequest request, @NonNull HttpServletResponse response,
                                  @NonNull FilterChain chain)
    throws ServletException, IOException {
    String token = resolveToken(request);
    if (token != null) {
      LoginUser loginUser = tokenManager.resolve(token);
      if (loginUser != null) {
        PreAuthenticatedAuthenticationToken authentication = new PreAuthenticatedAuthenticationToken(loginUser, null,
          List.<GrantedAuthority>of());
        authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
        SecurityContextHolder
          .getContext()
          .setAuthentication(authentication);
      }
    }
    chain.doFilter(request, response);
  }

  /**
   * 按 Bearer 规范取值；没有请求头、前缀不对、值为空都视为未携带 token。
   */
  private String resolveToken(HttpServletRequest request) {
    String header = request.getHeader(HttpHeaders.AUTHORIZATION);
    if (header == null || !header.startsWith(BEARER_PREFIX)) {
      return null;
    }
    String token = header
      .substring(BEARER_PREFIX.length())
      .trim();
    return token.isEmpty() ? null : token;
  }

}

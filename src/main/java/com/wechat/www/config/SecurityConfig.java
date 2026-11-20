package com.wechat.www.config;

import com.wechat.www.common.security.RestAuthenticationEntryPoint;
import com.wechat.www.common.security.TokenAuthenticationFilter;
import com.wechat.www.manager.TokenManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.ProviderNotFoundException;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import tools.jackson.databind.ObjectMapper;

/**
 * 安全过滤链：无状态 token 鉴权。
 *
 * <p>关闭 CSRF、表单登录、HTTP Basic 和 Spring Security 自带的 logout ——
 * 本项目的登录态完全由 {@link TokenManager} 的 Redis 会话承载，
 * 由 {@link TokenAuthenticationFilter} 解析，不使用任何 Security 内置认证方式。
 */
@Configuration
public class SecurityConfig {

  @Bean
  public SecurityFilterChain securityFilterChain(HttpSecurity http, TokenManager tokenManager,
      ObjectMapper objectMapper) throws Exception {
    http
      .csrf(AbstractHttpConfigurer::disable)
      .formLogin(AbstractHttpConfigurer::disable)
      .httpBasic(AbstractHttpConfigurer::disable)
      // 禁用内置登出：/api/auth/logout 走自己的 Controller，才能清理 Redis 会话
      .logout(AbstractHttpConfigurer::disable)
      .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
      .authorizeHttpRequests(auth -> auth
        // 登录、注册、发验证码必须匿名可访问
        .requestMatchers("/api/auth/login/**", "/api/user/register", "/api/sms/**").permitAll()
        // 接口文档（Knife4j / springdoc）
        .requestMatchers("/doc.html", "/webjars/**", "/v3/api-docs/**", "/swagger-ui/**", "/favicon.ico").permitAll()
        // 探活与错误页：/error 不放行会让异常转发再次被拦截，掩盖真实错误
        .requestMatchers("/actuator/health", "/actuator/info", "/error").permitAll()
        .anyRequest().authenticated())
      .exceptionHandling(handler -> handler.authenticationEntryPoint(new RestAuthenticationEntryPoint(objectMapper)))
      .addFilterBefore(new TokenAuthenticationFilter(tokenManager), UsernamePasswordAuthenticationFilter.class);
    return http.build();
  }

  /**
   * 阻止 Spring Boot 生成默认的 in-memory 用户。
   *
   * <p>{@code UserDetailsServiceAutoConfiguration} 在容器里找不到 AuthenticationManager
   * 时会创建一个用户名为 user、密码随机生成的账号，并在启动日志里打印该密码。
   * 本项目认证完全由 token 过滤器完成，不需要这个账号，这里显式提供一个
   * 会直接失败的 AuthenticationManager 来抑制自动配置。
   *
   * <p>如果将来有人误开 formLogin / httpBasic，会立刻得到这个明确的异常，
   * 而不是莫名其妙地接受一个内存账号。
   */
  @Bean
  public AuthenticationManager authenticationManager() {
    return authentication -> {
      throw new ProviderNotFoundException("本项目使用 token 鉴权，不支持用户名密码认证");
    };
  }

}

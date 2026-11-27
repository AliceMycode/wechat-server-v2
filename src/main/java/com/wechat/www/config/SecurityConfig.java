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
    http.csrf(AbstractHttpConfigurer::disable)
      .formLogin(AbstractHttpConfigurer::disable)
      .httpBasic(AbstractHttpConfigurer::disable)
      // 禁用内置登出：/api/auth/logout 走自己的 Controller，才能清理 Redis 会话
      .logout(AbstractHttpConfigurer::disable)
      .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
      .authorizeHttpRequests(auth -> auth
        // 登录、注册、发验证码必须匿名可访问
        .requestMatchers("/api/auth/login/**", "/api/user/register", "/api/sms/**")
        .permitAll()
        // 接口文档（Knife4j / springdoc）
        .requestMatchers("/doc.html", "/webjars/**", "/v3/api-docs/**", "/swagger-ui/**", "/favicon.ico")
        .permitAll()
        // 探活与错误页：/error 不放行会让异常转发再次被拦截，掩盖真实错误
        .requestMatchers("/actuator/health", "/actuator/info", "/error")
        .permitAll()
        .anyRequest()
        .authenticated())
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

 /* WebAsyncManagerIntegrationFilter：确保在异步请求的线程中，也能访问到当前的安全上下文（SecurityContext）。

  SecurityContextPersistenceFilter：负责安全上下文的持久化。它在请求开始时从 Session 等存储中加载上下文，并在请求结束时保存回去。

  HeaderWriterFilter：自动向响应中添加安全相关的 HTTP 头，如 X-Frame-Options、X-XSS-Protection 等，以增强安全性。

  CsrfFilter：处理跨站请求伪造（CSRF）防护，验证修改型请求（如 POST）中是否包含有效的 CSRF Token。

  LogoutFilter：拦截注销请求（默认 /logout），执行清理安全上下文、销毁 Session 等退出逻辑。

  UsernamePasswordAuthenticationFilter：处理表单登录的认证请求（默认 /login），提取用户名和密码进行认证。

  DefaultLoginPageGeneratingFilter：如果你没有自定义登录页，它会自动生成一个默认的登录表单页面。

  BasicAuthenticationFilter：解析 HTTP 请求头中的 Authorization: Basic 信息，用于 HTTP Basic 认证场景。

  RequestCacheAwareFilter：当用户被拦截去登录时，它负责缓存原始请求；登录成功后，再恢复该请求，让用户回到最初想访问的页面。

  SecurityContextHolderAwareRequestFilter：对 HttpServletRequest 进行包装，使其能更方便地获取安全上下文信息（如 getUserPrincipal()）。

  AnonymousAuthenticationFilter：如果前面的过滤器都没有建立认证信息，它会创建一个“匿名用户”放入上下文，确保后续流程能统一处理未登录请求。

  SessionManagementFilter：处理会话管理相关逻辑，如会话固定攻击防护、并发会话控制（限制同一用户登录数）等。

  ExceptionTranslationFilter：捕获链中抛出的安全异常（如 AuthenticationException、AccessDeniedException），并将其转换为对用户友好的响应（如 401 或
  403），或引导用户去登录。

  FilterSecurityInterceptor：过滤器链的最后一环，负责最终的授权决策。它根据配置的 URL 安全规则，判断当前用户是否有权访问目标资源。*/

}

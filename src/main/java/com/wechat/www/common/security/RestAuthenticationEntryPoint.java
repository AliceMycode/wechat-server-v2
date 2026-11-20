package com.wechat.www.common.security;

import com.wechat.www.common.exception.ErrorCodeEnum;
import com.wechat.www.common.result.Result;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.NonNull;
import org.springframework.http.MediaType;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import tools.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

/**
 * 未认证访问受保护接口时的响应：401 + 项目统一的 {@link Result} JSON。
 *
 * <p>Spring Security 的认证异常发生在过滤器链里，{@code @RestControllerAdvice}
 * 捕获不到，必须由这里输出，否则前端拿到的是空响应体的默认 401。
 */
@RequiredArgsConstructor
public class RestAuthenticationEntryPoint implements AuthenticationEntryPoint {

  private final ObjectMapper objectMapper;

  @Override
  public void commence(@NonNull HttpServletRequest request, HttpServletResponse response,
                       @NonNull AuthenticationException authException) throws IOException {
    response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
    response.setContentType(MediaType.APPLICATION_JSON_VALUE);
    response.setCharacterEncoding(StandardCharsets.UTF_8.name());
    String body = objectMapper.writeValueAsString(
      Result.fail(ErrorCodeEnum.UNAUTHORIZED, ErrorCodeEnum.UNAUTHORIZED.getMessage()));
    response
      .getWriter()
      .write(body);
  }

}

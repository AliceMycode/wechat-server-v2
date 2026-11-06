package com.wechat.www.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * OpenAPI 3 / Swagger UI 配置。
 */
@Configuration
public class OpenApiConfig {

  @Bean
  public OpenAPI wechatOpenApi() {
    return new OpenAPI().info(new Info()
      .title("wechat-server-v2 API")
      .version("v1")
      .description("微信风格 IM 服务端 API"));
  }

}

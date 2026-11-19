package com.wechat.www.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

/**
 * 密码编码配置。
 *
 * <p>选用 BCrypt：哈希串内部自带随机盐（格式 {@code $2a$10$} + 22 字符盐 + 31 字符哈希，共 60 字符），
 * 数据库无需单独维护盐字段；验证时从串中解析盐重算比对，同一密码每次编码结果都不同。
 *
 * <p>只引入 spring-security-crypto，不引入 spring-boot-starter-security，
 * 因此本 Bean 不会带来任何 HTTP 认证过滤链。
 */
@Configuration
public class PasswordConfig {

  @Bean
  public PasswordEncoder passwordEncoder() {
    // cost 默认 10（2^10 轮），单次校验几十毫秒，兼顾安全与注册/登录响应时间
    return new BCryptPasswordEncoder();
  }

}

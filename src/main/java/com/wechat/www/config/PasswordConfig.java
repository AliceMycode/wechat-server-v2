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
 * <p>{@link BCryptPasswordEncoder} 来自 spring-security-crypto，随 starter-security 传递引入，
 * 无需单独声明依赖。密码哈希与 HTTP 认证过滤链是两条互不相干的链路：
 * 本 Bean 只做编码与比对，不参与任何请求拦截。
 */
@Configuration
public class PasswordConfig {

  @Bean
  public PasswordEncoder passwordEncoder() {
    // cost 默认 10（2^10 轮），单次校验几十毫秒，兼顾安全与注册/登录响应时间
    return new BCryptPasswordEncoder();
  }

}

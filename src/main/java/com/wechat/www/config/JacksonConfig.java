package com.wechat.www.config;

import org.springframework.boot.jackson.autoconfigure.JsonMapperBuilderCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import tools.jackson.core.JsonGenerator;
import tools.jackson.databind.SerializationContext;
import tools.jackson.databind.module.SimpleModule;
import tools.jackson.databind.ser.std.StdSerializer;

/**
 * Jackson 序列化配置。
 *
 * <p>Spring Boot 4 升级到 Jackson 3（tools.jackson），自定义序列化通过
 * {@link JsonMapperBuilderCustomizer} 注入，替代旧版的 Jackson2ObjectMapperBuilderCustomizer。
 */
@Configuration
public class JacksonConfig {

  /** JavaScript 安全整数上限 2^53 - 1，超过此范围的整数 JSON.parse 会丢失末尾精度 */
  private static final long JS_MAX_SAFE_INTEGER = 9007199254740991L;

  /**
   * 超出 JS 安全整数范围的 Long 序列化为字符串，范围内保持数字。
   *
   * <p>雪花 ID 是 64 位整数（约 18~19 位十进制），必然超出 JS 安全整数范围（2^53 - 1，16 位），
   * 浏览器 JSON.parse 会丢精度，必须按字符串输出（表结构约定见 V1__init_schema.sql 头部注释）；
   * 分页 total、计数等业务数字远小于该范围，保持数字输出，前端无需特殊处理。
   *
   * <p>按数值大小动态判断而非逐字段注解：零注解、零遗漏，请求入参不受影响
   * （前端传数字或字符串 Spring 都能绑定到 Long）。
   */
  @Bean
  public JsonMapperBuilderCustomizer longSafeToStringCustomizer() {
    SimpleModule longSafeModule = new SimpleModule();
    LongOutOfRangeToStringSerializer serializer = new LongOutOfRangeToStringSerializer();
    longSafeModule.addSerializer(Long.class, serializer);
    longSafeModule.addSerializer(Long.TYPE, serializer);
    return builder -> builder.addModule(longSafeModule);
  }

  /**
   * 超出 JS 安全整数范围的 Long 输出字符串，范围内保持数字。
   */
  private static final class LongOutOfRangeToStringSerializer extends StdSerializer<Long> {

    private LongOutOfRangeToStringSerializer() {
      super(Long.class);
    }

    @Override
    public void serialize(Long value, JsonGenerator gen, SerializationContext ctxt) {
      if (value > JS_MAX_SAFE_INTEGER || value < -JS_MAX_SAFE_INTEGER) {
        gen.writeString(Long.toString(value));
      } else {
        gen.writeNumber(value);
      }
    }
  }

}

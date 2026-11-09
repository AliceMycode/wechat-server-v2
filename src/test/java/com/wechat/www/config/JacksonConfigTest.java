package com.wechat.www.config;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import tools.jackson.databind.ObjectMapper;

/**
 * Jackson 序列化配置测试：超出 JS 安全整数范围的 Long 输出字符串，范围内保持数字。
 */
@SpringBootTest
class JacksonConfigTest {

  @Autowired
  private ObjectMapper objectMapper;

  @Test
  void snowflakeIdShouldSerializeAsString() {
    // 雪花 ID 量级的 64 位整数，超过 JS 安全整数范围（2^53-1）
    long snowflakeId = 726400000000000001L;
    assertThat(objectMapper.writeValueAsString(snowflakeId)).isEqualTo("\"726400000000000001\"");
  }

  @Test
  void smallLongShouldSerializeAsNumber() {
    // 计数类小数字保持在安全范围内，按数字输出
    assertThat(objectMapper.writeValueAsString(123L)).isEqualTo("123");
    assertThat(objectMapper.writeValueAsString(Long.valueOf(123L))).isEqualTo("123");
  }

  @Test
  void boundaryShouldSerializeAsNumberAtSafeMax() {
    // 边界：恰好 2^53-1 仍是安全整数，按数字输出
    assertThat(objectMapper.writeValueAsString(9007199254740991L)).isEqualTo("9007199254740991");
    // 边界 +1 超出安全范围，转字符串
    assertThat(objectMapper.writeValueAsString(9007199254740992L)).isEqualTo("\"9007199254740992\"");
  }
}

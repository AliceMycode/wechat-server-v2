package com.wechat.www.config;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.serializer.RedisSerializer;
import org.springframework.data.redis.serializer.SerializationException;

import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Redis JSON 序列化器行为测试：确认哪些数据结构能安全往返。
 *
 * <p>重点是 {@link Map#of} 生成的不可变 Map——JSON 序列化会记录实际类名
 * （如 {@code java.util.ImmutableCollections$Map1}），反序列化时可能无法实例化。
 * 本测试用于确认边界，避免以后在 Redis 里存了读不回来的数据。
 */
class RedisConfigTest {

  private final RedisSerializer<Object> serializer = RedisSerializer.json();

  @Test
  @DisplayName("POJO 可完整往返")
  void pojoShouldRoundTrip() {
    Sample sample = new Sample(726400000000000001L, "device-1");

    Object restored = serializer.deserialize(serializer.serialize(sample));

    assertThat(restored).isEqualTo(sample);
  }

  @Test
  @DisplayName("HashMap 可完整往返")
  void hashMapShouldRoundTrip() {
    Map<String, Object> map = new HashMap<>();
    map.put("userId", 1L);
    map.put("deviceId", "device-1");

    Object restored = serializer.deserialize(serializer.serialize(map));

    assertThat(restored).isInstanceOf(Map.class);
    Map<?, ?> restoredMap = (Map<?, ?>) restored;
    assertThat(restoredMap.get("userId")).isEqualTo(1L);
    assertThat(restoredMap.get("deviceId")).isEqualTo("device-1");
  }

  @Test
  @DisplayName("Map.of 生成的不可变 Map 无法反序列化")
  void immutableMapCannotBeReadBack() {
    Map<String, Object> map = Map.of("userId", 1L, "deviceId", "device-1");
    byte[] bytes = serializer.serialize(map);

    // Map.of 生成的不可变 Map 不带 @class 类型信息，反序列化时无法确定目标类型。
    // 存 Hash 结构请用 HashMap 或 POJO，不要用 Map.of。
    assertThatThrownBy(() -> serializer.deserialize(bytes)).isInstanceOf(SerializationException.class);
  }

  record Sample(Long userId, String deviceId) {
  }

}

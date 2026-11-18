package com.wechat.www.domain.enums;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.wechat.www.common.exception.BizException;
import com.wechat.www.common.exception.ErrorCodeEnum;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * 短信场景枚举解析：合法值正常返回，非法值统一按参数错误拒绝。
 */
class SmsSceneEnumTest {

  @Test
  @DisplayName("合法场景标识能解析为对应枚举")
  void of_whenCodeSupported_shouldReturnEnum() {
    assertThat(SmsSceneEnum.of("register")).isEqualTo(SmsSceneEnum.REGISTER);
    assertThat(SmsSceneEnum.of("login")).isEqualTo(SmsSceneEnum.LOGIN);
    assertThat(SmsSceneEnum.of("reset")).isEqualTo(SmsSceneEnum.RESET);
  }

  @Test
  @DisplayName("不支持的场景标识抛 SCENE_INVALID")
  void of_whenCodeUnsupported_shouldThrow() {
    BizException ex = assertThrows(BizException.class, () -> SmsSceneEnum.of("unknown"));

    assertThat(ex.getErrorCode()).isEqualTo(ErrorCodeEnum.SCENE_INVALID);
  }

  @Test
  @DisplayName("场景为 null 时同样按非法值拒绝")
  void of_whenCodeNull_shouldThrow() {
    BizException ex = assertThrows(BizException.class, () -> SmsSceneEnum.of(null));

    assertThat(ex.getErrorCode()).isEqualTo(ErrorCodeEnum.SCENE_INVALID);
  }

}

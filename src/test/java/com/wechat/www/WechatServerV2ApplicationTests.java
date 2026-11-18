package com.wechat.www;

import com.wechat.www.manager.SmsCodeChannel;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

@SpringBootTest
class WechatServerV2ApplicationTests {

  /**
   * 短信渠道 bean 在测试配置下不存在（测试 application.yml 未定义 sms.mode），
   * 用 mock 顶替，既能验证上下文装配，又不依赖阿里云配置与网络。
   */
  @MockitoBean
  private SmsCodeChannel smsCodeChannel;

  @Test
  void contextLoads() {
  }
}

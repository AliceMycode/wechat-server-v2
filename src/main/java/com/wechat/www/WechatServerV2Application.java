package com.wechat.www;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@MapperScan("com.wechat.www.mapper")
public class WechatServerV2Application {

  public static void main(String[] args) {
    SpringApplication.run(WechatServerV2Application.class, args);
  }

}

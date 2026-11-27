package com.wechat.www.domain.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 短信发送记录 DO（对应表 sms_record）。
 */
@Data
@TableName("sms_record")
public class SmsRecordDO {

  /**
   * 自增ID
   */
  @TableId(type = IdType.AUTO)
  private Long id;

  /**
   * 手机号
   */
  private String phone;

  /**
   * 场景 register/login/reset（取值见 SmsSceneEnum）
   */
  private String scene;

  /**
   * 发送回执ID（渠道方返回，用于对账）
   */
  private String bizId;

  /**
   * 请求ID（渠道方返回）
   */
  private String requestId;

  /**
   * 状态 0发送中 1成功 2失败
   */
  private Integer sendStatus;

  /**
   * 错误码
   */
  private String errorCode;

  /**
   * 请求IP（风控审计）
   */
  private String ip;

  /**
   * 设备标识
   */
  private String deviceId;

  /**
   * 创建时间：数据库 DEFAULT CURRENT_TIMESTAMP，不在代码里赋值
   */
  private LocalDateTime createTime;

  /**
   * 更新时间：数据库 ON UPDATE CURRENT_TIMESTAMP，不在代码里赋值
   */
  private LocalDateTime updateTime;

}

package com.wechat.www.domain.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 用户设备 DO（对应表 user_device）。
 */
@Data
@TableName("user_device")
public class UserDeviceDO {

  /**
   * 自增ID
   */
  @TableId(type = IdType.AUTO)
  private Long id;

  /**
   * 用户ID
   */
  private Long userId;

  /**
   * 设备唯一标识：客户端首次启动生成并本地持久化的 UUID
   */
  private String deviceId;

  /**
   * 设备类型 1Electron 2iOS 3Android 4Web；未识别为 0
   */
  private Integer deviceType;

  /**
   * 设备名称
   */
  private String deviceName;

  /**
   * 推送token
   */
  private String pushToken;

  /**
   * 推送通道 0无 1APNs 2华为 3小米 4OPPO 5vivo 6荣耀 7EMAS
   */
  private Integer pushProvider;

  /**
   * 最后活跃时间
   */
  private LocalDateTime lastActiveTime;

  /**
   * 状态 0正常 1禁用
   */
  private Integer status;

  /**
   * 创建时间：数据库 DEFAULT CURRENT_TIMESTAMP，不在代码里赋值
   */
  private LocalDateTime createTime;

  /**
   * 更新时间：数据库 ON UPDATE CURRENT_TIMESTAMP，不在代码里赋值
   */
  private LocalDateTime updateTime;

  /**
   * 逻辑删除 1是 0否：查询自动追加 is_deleted = 0
   */
  @TableLogic
  private Integer isDeleted;

}

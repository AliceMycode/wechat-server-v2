package com.wechat.www.domain;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 用户资料 DO（对应表 user_profile）。
 */
@Data
@TableName("user_profile")
public class UserProfileDO {

  /**
   * 自增ID
   */
  @TableId(type = IdType.AUTO)
  private Long id;

  /**
   * 用户ID：与 user_account.id 一一对应（uk_user_id 唯一）
   */
  private Long userId;

  /**
   * 昵称
   */
  private String nickname;

  /**
   * 头像文件ID
   */
  private Long avatarFileId;

  /**
   * 个性签名
   */
  private String signature;

  /**
   * 性别 0未知 1男 2女
   */
  private Integer gender;

  /**
   * 地区
   */
  private String area;

  /**
   * 生日
   */
  private LocalDate birthday;

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

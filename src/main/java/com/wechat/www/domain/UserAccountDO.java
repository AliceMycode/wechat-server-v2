package com.wechat.www.domain;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 用户账号 DO（对应表 user_account）。
 */
@Data
@TableName("user_account")
public class UserAccountDO {

  /** 用户ID（雪花ID，插入时由 MyBatis-Plus 生成并回填） */
  @TableId(type = IdType.ASSIGN_ID)
  private Long id;

  /** 国家区号 */
  private String countryCode;

  /** 手机号 */
  private String phone;

  /** 密码哈希（BCrypt，自带随机盐，固定 60 字符） */
  private String passwordHash;

  /** 状态 0正常 1禁用 2注销 */
  private Integer status;

  /** 最后登录时间 */
  private LocalDateTime lastLoginTime;

  /** 创建时间：数据库 DEFAULT CURRENT_TIMESTAMP，不在代码里赋值 */
  private LocalDateTime createTime;

  /** 更新时间：数据库 ON UPDATE CURRENT_TIMESTAMP，不在代码里赋值 */
  private LocalDateTime updateTime;

  /** 逻辑删除 1是 0否：查询自动追加 is_deleted = 0 */
  @TableLogic
  private Integer isDeleted;

}

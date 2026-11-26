package com.wechat.www.domain.vo;

import com.wechat.www.domain.UserProfileDO;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 当前登录用户资料（GET /api/user/me 的返回体）。
 *
 * <p>只暴露前端需要的字段：主键 id、逻辑删除标记、更新时间属于内部字段，不回传。
 */
@Data
public class UserProfileVO {

  /**
   * 用户ID（雪花ID，JSON 中以字符串返回避免精度丢失）
   */
  @Schema(description = "用户ID（雪花ID，JSON 中以字符串返回避免精度丢失）", example = "726400000000000001")
  private Long userId;

  /**
   * 昵称
   */
  @Schema(description = "昵称", example = "微信用户001")
  private String nickname;

  /**
   * 头像文件ID，未设置时为 null
   */
  @Schema(description = "头像文件ID，未设置时为 null", example = "726400000000000002")
  private Long avatarFileId;

  /**
   * 个性签名
   */
  @Schema(description = "个性签名", example = "这是一条个性签名")
  private String signature;

  /**
   * 性别 0未知 1男 2女
   */
  @Schema(description = "性别 0未知 1男 2女", example = "0")
  private Integer gender;

  /**
   * 地区
   */
  @Schema(description = "地区", example = "北京市")
  private String area;

  /**
   * 生日
   */
  @Schema(description = "生日", example = "2000-01-01")
  private LocalDate birthday;

  /**
   * 注册时间
   */
  @Schema(description = "注册时间", example = "2026-01-01T12:00:00")
  private LocalDateTime createTime;

  /**
   * 从 DO 转为对外 VO：逐字段拷贝，不用反射工具，字段增减时编译器会提示。
   */
  public static UserProfileVO from(UserProfileDO profile) {
    UserProfileVO vo = new UserProfileVO();
    vo.setUserId(profile.getUserId());
    vo.setNickname(profile.getNickname());
    vo.setAvatarFileId(profile.getAvatarFileId());
    vo.setSignature(profile.getSignature());
    vo.setGender(profile.getGender());
    vo.setArea(profile.getArea());
    vo.setBirthday(profile.getBirthday());
    vo.setCreateTime(profile.getCreateTime());
    return vo;
  }

}
package com.wechat.www.common.security;

/**
 * 当前登录用户，作为 Spring Security {@code Authentication#getPrincipal()} 的内容，
 * Controller 用 {@code @AuthenticationPrincipal} 直接取用。
 *
 * <p>只放鉴权链路必需的两个字段：用户ID 与设备标识。昵称、头像等展示信息
 * 由业务接口按需查询，避免每张表都塞进凭证。
 *
 * @param userId   用户ID
 * @param deviceId 设备标识
 */
public record LoginUser(Long userId, String deviceId) {
}

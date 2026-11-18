package com.wechat.www.domain.dto;

/**
 * 短信渠道发送结果（Manager 层向上传输的对象）。
 *
 * @param bizId     渠道回执 ID，落库 sms_record.biz_id，用于发送状态对账
 * @param requestId 渠道请求 ID，落库 sms_record.request_id，可据此在阿里云控制台查调用日志
 */
public record SmsSendResultDTO(String bizId, String requestId) {
}

//编译器自动展开成：
//public final class SmsSendResultDTO extends java.lang.Record {
//  private final String bizId;
//  private final String requestId;
//  public SmsSendResultDTO(String bizId, String requestId) { ... }
//  public String bizId() { return bizId; }        // 注意：不是 getBizId()
//  public String requestId() { return requestId; }
//  public boolean equals(Object o) { ... }
//  public int hashCode() { ... }
//  public String toString() { ... }
//}

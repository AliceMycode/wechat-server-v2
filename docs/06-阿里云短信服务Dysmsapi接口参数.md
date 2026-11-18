# 06 - 阿里云短信服务（Dysmsapi）接口参数

> 来源：阿里云 API 文档整理，产品 Dysmsapi version 2017-05-25，endpoint `dysmsapi.aliyuncs.com`（RPC 风格）。
> 对应实现：`AliyunDysmsCodeChannelImpl`（sms.mode=dysmsapi，**当前预留未启用**，启用需企业资质签名）。
> 与认证服务（Dypnsapi，见 docs/05）是两个独立产品、独立 SDK（dysmsapi20170525 vs dypnsapi20170525）。

## SendSms（发送短信）

文档地址：https://help.aliyun.com/zh/sms/developer-reference/api-dysmsapi-2017-05-25-sendsms

QPS 上限 5000/秒。验证码类短信**建议单条发送**。

| 参数 | 类型 | 必填 | 说明 | 本项目处理 |
|---|---|---|---|---|
| PhoneNumbers | string | ✅ | 手机号，支持逗号分隔批量（上限 1000，有延迟；验证码建议单发） | 入参传入，单发 |
| SignName | string | ✅ | 签名名称，必须审核通过 | `sms.dysms.sign-name` |
| TemplateCode | string | ✅ | 模板 CODE，必须审核通过（SMS_xxx 格式） | `sms.dysms.template-code` |
| TemplateParam | string | ❌ | 模板变量 JSON（模板含变量时必填），如 `{"code":"123456"}` | 代码拼接 `{"code":"<验证码>"}` |
| SmsUpExtendCode | string | ❌ | 上行短信扩展码，系统自动生成，不支持自行传入 | 不接，无需求 |
| OutId | string | ❌ | 外部流水扩展字段 | 不接，无需求 |

与认证服务的关键差异：**验证码由本服务生成**（`AliyunDysmsCodeChannelImpl` 自己生成、Redis 托管核验），
`TemplateParam` 传真实验证码值，没有 `##code##` 占位符/CodeType/CodeLength 概念。

### 出参

| 字段 | 说明 | 本项目处理 |
|---|---|---|
| Code | 请求状态码，`OK` 代表成功 | **必须校验 `== "OK"`**（HTTP 200 ≠ 发送成功），实现已处理 |
| Message | 状态码描述 | 错误时进 BizException message |
| BizId | 发送回执 ID，可用于 QuerySendDetails 查发送状态 | 落库 `sms_record.biz_id` |
| RequestId | 请求 ID | 日志排查 |

### 注意事项（官方）

- 国内短信超时时间建议 ≥1s；超时失败先查回执状态再决定是否重试
- 发送接口**不支持幂等**，应用侧做好防重（本项目用 60 秒冷却 key 保证）
- 计费口径：按运营商回执状态计费，提交成功但回执失败不计费
- 验证码类型短信建议单条发送（群发有延迟）

## SendBatchSms（批量发送）——不接入

文档地址：https://help.aliyun.com/zh/sms/developer-reference/api-dysmsapi-2017-05-25-sendbatchsms

单次最多 100 个手机号，给不同号码发不同签名/同模板的短信，参数为 JSON 数组
（PhoneNumberJson / SignNameJson / TemplateParamJson，数量须一一对应）。
**验证码场景官方明确建议用 SendSms 单发**，本项目验证码服务不接入此接口。

## QuerySendDetails（查询发送详情）——未接入，备用

文档地址：https://help.aliyun.com/zh/sms/developer-reference/api-dysmsapi-2017-05-25-querysenddetails

查询单个手机号最近 30 天的发送记录/状态，可传 `BizId` 查指定记录。仅支持单号码查询。

入参：PhoneNumber（必填）、SendDate（必填，yyyyMMdd）、PageSize（必填，1~50）、CurrentPage（必填）、BizId（可选）。

出参关键字段：`TotalCount` + `SmsSendDetailDTOs.SmsSendDetailDTO[]`，每条明细含：

- `SendStatus`：1-等待回执 / 2-发送失败 / 3-发送成功
- `ErrCode`：运营商状态码，成功为 `DELIVERED`
- `Content`：短信内容、`SendDate`/`ReceiveDate`：发送/接收时间

用途：后续做发送状态对账、失败补偿时可接入；当前排查依赖控制台 + `sms_record`。

## 实现核对结论

`AliyunDysmsCodeChannelImpl` 与本文档一致：

- 请求：PhoneNumbers/SignName/TemplateCode/TemplateParam 四项 ✅
- 成败判断：`body.getCode() == "OK"` ✅（对应"HTTP 200 ≠ 成功"约定）
- 回执：`body.getBizId()` 落库 ✅

预留模式的启用路径：企业资质审核通过 → 申请签名/模板 → 配置 `sms.dysms.sign-name`/`template-code` → `sms.mode` 改为 dysmsapi，业务代码零改动。

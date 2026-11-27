package com.wechat.www.common.constant;

/**
 * 阿里云短信渠道常量。渠道协议约定值统一收敛在此，
 * 禁止散落在各实现类中（规约【强制】：不允许魔法值直接出现在代码里）。
 */
public final class AliyunSmsConst {

  /**
   * 阿里云云通信号码认证服务（Dypnsapi）端点
   */
  public static final String AUTH_ENDPOINT = "dypnsapi.aliyuncs.com";

  /**
   * 阿里云短信服务（Dysmsapi）端点
   */
  public static final String DYSMS_ENDPOINT = "dysmsapi.aliyuncs.com";

  /**
   * SDK 连接超时（毫秒）：远端操作必须有超时设置，否则线程会被慢连接拖死
   */
  public static final int CONNECT_TIMEOUT_MILLIS = 3000;

  /**
   * SDK 读取超时（毫秒）：短信网关偶发抖动，读取窗口放宽于连接
   */
  public static final int READ_TIMEOUT_MILLIS = 5000;

  /**
   * 认证服务核验通过的结果值（阿里云文档约定，不通过为 REJECT）
   */
  public static final String AUTH_VERIFY_RESULT_PASS = "PASS";

  /**
   * 认证服务模板参数里验证码的占位符：阿里云动态生成验证码并托管核验（验证码不经过本服务）
   */
  public static final String AUTH_CODE_PLACEHOLDER = "##code##";

  /**
   * 传统短信业务成功码：阿里云约定 body.code == "OK" 才算发送成功
   */
  public static final String DYSMS_SUCCESS_CODE = "OK";

  /**
   * 传统短信验证码取值上界：nextInt(1_000_000) 产生 0~999999，再格式化补齐前导零成 6 位
   */
  public static final int DYSMS_CODE_UPPER_BOUND = 1_000_000;

  /**
   * 传统短信验证码格式：6 位数字，前导零补齐；位数必须与 DYSMS_CODE_UPPER_BOUND 保持一致
   */
  public static final String DYSMS_CODE_FORMAT = "%06d";

  private AliyunSmsConst() {
  }

}

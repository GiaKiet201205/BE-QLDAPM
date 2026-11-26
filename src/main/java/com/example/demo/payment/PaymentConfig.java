package com.example.demo.payment;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import java.net.URI;

@Configuration
@EnableConfigurationProperties(PaymentConfig.Settings.class)
public class PaymentConfig {
 @Data @ConfigurationProperties(prefix="payment")
 public static class Settings {
  private boolean sandbox=true;
  private String frontendUrl="http://localhost:5173",publicApiUrl="";
  private String vnpayTmnCode="",vnpayHashSecret="";
  private String zalopayAppId="",zalopayKey1="",zalopayKey2="";
  private String zalopayCreateUrl="",zalopayQueryUrl="",zalopayRedirectUrl="",zalopayCallbackUrl="";
  public String frontend(){return origin(frontendUrl,true);}
  public String api(){return origin(publicApiUrl,false);}
  public boolean configured(String provider){
   try{
    frontend();
    return switch(provider){
     case "VNPAY" -> { if(!sandbox)api(); yield !vnpayTmnCode.isBlank()&&!vnpayHashSecret.isBlank(); }
     case "ZALOPAY" -> {
      if(!sandbox)yield false;
      zalopayCreate();zalopayQuery();zalopayRedirect();zalopayCallback();
      yield zalopayAppId.matches("[1-9][0-9]{0,8}")&&zalopayKey1.length()>=16&&zalopayKey2.length()>=16;
     }
     default -> false;
    };
   }catch(IllegalArgumentException ex){return false;}
  }
  public String vnpayUrl(){return sandbox?"https://sandbox.vnpayment.vn/paymentv2/vpcpay.html":"https://pay.vnpay.vn/vpcpay.html";}
  public String zalopayCreate(){return zalopayEndpoint(zalopayCreateUrl,"/v2/create");}
  public String zalopayQuery(){return zalopayEndpoint(zalopayQueryUrl,"/v2/query");}
  private String zalopayEndpoint(String configured,String path){
   String expected="https://sb-openapi.zalopay.vn"+path;
   if(!sandbox||!configured.isBlank()&&!expected.equals(configured.trim()))
    throw new IllegalArgumentException("ZaloPay chỉ hỗ trợ endpoint Sandbox đã xác định.");
   return expected;
  }
  public String zalopayRedirect(){
   String expected=frontend()+"/api/payments/zalopay/return";
   if(!zalopayRedirectUrl.isBlank()&&!expected.equals(zalopayRedirectUrl.trim()))
    throw new IllegalArgumentException("Địa chỉ quay về ZaloPay phải thuộc website hiện tại.");
   return expected;
  }
  public String zalopayCallback(){
   // Localhost có thể đối chiếu qua API query; callback chỉ dùng khi có HTTPS công khai.
   if(zalopayCallbackUrl.isBlank())return publicApiUrl.isBlank()?"":api()+"/api/payments/zalopay/callback";
   URI uri=URI.create(zalopayCallbackUrl.trim());
   if(!"/api/payments/zalopay/callback".equals(uri.getPath())||uri.getQuery()!=null||uri.getFragment()!=null
     ||uri.getPort()!=-1&&uri.getPort()!=443)throw new IllegalArgumentException("Địa chỉ callback ZaloPay không hợp lệ.");
   return origin(uri.resolve("/").toString(),false)+"/api/payments/zalopay/callback";
  }
  private static String origin(String value,boolean allowLocal){
   try{var uri=URI.create(value);String host=uri.getHost();boolean local=host!=null&&java.util.Set.of("localhost","127.0.0.1","[::1]").contains(host);
    if(host==null||uri.getUserInfo()!=null||uri.getQuery()!=null||uri.getFragment()!=null||!(uri.getPath().isEmpty()||"/".equals(uri.getPath()))||!("https".equals(uri.getScheme())||allowLocal&&local&&"http".equals(uri.getScheme()))||!allowLocal&&local)throw new IllegalArgumentException();
    return value.replaceAll("/+$","");
   }catch(Exception ex){throw new IllegalArgumentException("Payment origin is not configured correctly.");}
  }
 }
}

package com.example.demo.payment;
import com.example.demo.exception.ApiException;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.math.BigDecimal;
import java.util.*;
import static com.example.demo.payment.PaymentDtos.*;
import static com.example.demo.payment.PaymentSignatures.*;

@Service @RequiredArgsConstructor
public class PaymentGateways {
 private final PaymentConfig.Settings config;
 public record Link(String url,String qr) {}
 public record Verified(String attemptId,String provider,BigDecimal amount,boolean success,boolean pending,String transactionId,String code) {}
 public Link create(Attempt attempt,String ip){
  if(!"VNPAY".equals(attempt.provider()))throw new ApiException(HttpStatus.BAD_REQUEST,"Phương thức thanh toán không hợp lệ.");
  return vnpay(attempt,ip);
 }
 private Link vnpay(Attempt a,String ip){
  DateTimeFormatter time=DateTimeFormatter.ofPattern("yyyyMMddHHmmss").withZone(ZoneId.of("Asia/Ho_Chi_Minh"));
  Map<String,String> p=new TreeMap<>();p.put("vnp_Version","2.1.0");p.put("vnp_Command","pay");p.put("vnp_TmnCode",config.getVnpayTmnCode());
  p.put("vnp_Amount",a.amount().multiply(BigDecimal.valueOf(100)).toBigIntegerExact().toString());p.put("vnp_CurrCode","VND");p.put("vnp_TxnRef",a.id());
  p.put("vnp_OrderInfo","Thanh toan hoc phi GK "+a.orderId());p.put("vnp_OrderType","other");p.put("vnp_Locale","vn");
  p.put("vnp_IpAddr",ip);p.put("vnp_CreateDate",time.format(a.createdAt()));p.put("vnp_ExpireDate",time.format(a.expiresAt()));p.put("vnp_ReturnUrl",config.frontend()+"/api/payments/vnpay/return");
  if(config.isSandbox())p.put("vnp_BankCode","NCB");
  String data=vnpayData(p),url=config.vnpayUrl()+"?"+data+"&vnp_SecureHash="+hmac("HmacSHA512",config.getVnpayHashSecret(),data);
  return new Link(url,url);
 }
 public Verified verifyVnpay(Map<String,String> p){
  if(!config.configured("VNPAY")||!config.getVnpayTmnCode().equals(p.get("vnp_TmnCode"))||!equal(hmac("HmacSHA512",config.getVnpayHashSecret(),vnpayData(p)),p.get("vnp_SecureHash")))throw new ApiException(HttpStatus.BAD_REQUEST,"Chữ ký thanh toán không hợp lệ.");
  String code=p.getOrDefault("vnp_ResponseCode",""),status=p.getOrDefault("vnp_TransactionStatus","");
  if(!code.matches("[0-9]{2}")||!status.matches("[0-9]{2}"))throw new ApiException(HttpStatus.BAD_REQUEST,"Thiếu kết quả thanh toán.");
  return new Verified(required(p,"vnp_TxnRef"),"VNPAY",amount(p,"vnp_Amount").movePointLeft(2),"00".equals(code)&&"00".equals(status),"01".equals(status),p.get("vnp_TransactionNo"),code);
 }
 private static String required(Map<String,String> p,String key){String value=p.get(key);if(value==null||value.isBlank()||value.length()>100)throw new ApiException(HttpStatus.BAD_REQUEST,"Thiếu thông tin giao dịch.");return value;}
 private static BigDecimal amount(Map<String,String> p,String key){String value=required(p,key);if(!value.matches("[0-9]{1,14}"))throw new ApiException(HttpStatus.BAD_REQUEST,"Số tiền không hợp lệ.");return new BigDecimal(value);}
}

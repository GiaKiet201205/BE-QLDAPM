package com.example.demo.payment;
import com.example.demo.exception.ApiException;
import org.junit.jupiter.api.Test;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static com.example.demo.payment.PaymentSignatures.*;

class PaymentGatewaysTest {
 static PaymentConfig.Settings config(){var c=new PaymentConfig.Settings();c.setPublicApiUrl("https://callbacks.example.test");c.setVnpayTmnCode("TESTCODE");c.setVnpayHashSecret("test-vnpay-secret");return c;}
 static Map<String,String> vnpay(String id,String amount){Map<String,String> p=new HashMap<>(Map.of("vnp_TxnRef",id,"vnp_Amount",amount,"vnp_TmnCode","TESTCODE","vnp_ResponseCode","00","vnp_TransactionStatus","00","vnp_TransactionNo","12345"));p.put("vnp_SecureHash",hmac("HmacSHA512","test-vnpay-secret",vnpayData(p)));return p;}
 final PaymentGateways gateway=new PaymentGateways(config());
 @Test void hmacUsesKnownSha256VectorAndConstantTimeComparison(){assertEquals("f7bc83f430538424b13298e6aa6fb143ef4d59a14946175997479dbc2d1a3cd8",hmac("HmacSHA256","key","The quick brown fox jumps over the lazy dog"));assertTrue(equal("abcdef","ABCDEF"));assertFalse(equal("abcdef","abcdeg"));}
 @Test void vnpayValidatesSignatureAmountScaleAndBothSuccessCodes(){var p=vnpay("paytest","249000000");assertEquals(new BigDecimal("2490000.00"),gateway.verifyVnpay(p).amount());assertTrue(gateway.verifyVnpay(p).success());p.put("vnp_Amount","100");var tampered=p;assertThrows(ApiException.class,()->gateway.verifyVnpay(tampered));p=vnpay("paytest","249000000");p.put("vnp_TransactionStatus","02");p.put("vnp_SecureHash",hmac("HmacSHA512","test-vnpay-secret",vnpayData(p)));assertFalse(gateway.verifyVnpay(p).success());}
 @Test void sandboxAllowsLocalReturnWhileProductionRequiresPublicCallback(){
  var c=new PaymentConfig.Settings();assertFalse(c.configured("VNPAY"));
  c=config();c.setPublicApiUrl("http://localhost:8080");
  assertTrue(c.configured("VNPAY"));
  c.setSandbox(false);assertFalse(c.configured("VNPAY"));
  c.setPublicApiUrl("https://callbacks.example.test");assertTrue(c.configured("VNPAY"));
  c.setPublicApiUrl("");assertFalse(c.configured("VNPAY"));
 }
 @Test void generatedVnpayLinkUsesSandboxSignedReturnAndExpiry(){var now=Instant.parse("2026-11-20T12:00:00Z");var a=new PaymentDtos.Attempt("paytest","gk-test","VNPAY",new BigDecimal("2490000"),"CREATING",null,null,null,now,now.plusSeconds(900));var link=gateway.create(a,"127.0.0.1");assertTrue(link.url().startsWith("https://sandbox.vnpayment.vn/"));assertTrue(link.url().contains("vnp_Amount=249000000"));assertTrue(link.url().contains("vnp_CreateDate=20261120190000"));assertTrue(link.url().contains("vnp_ExpireDate=20261120191500"));assertEquals(link.url(),link.qr());assertFalse(link.url().contains("test-vnpay-secret"));}
}

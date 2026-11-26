package com.example.demo.payment;

import com.example.demo.exception.ApiException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

import static com.example.demo.payment.PaymentDtos.Attempt;
import static com.example.demo.payment.PaymentGateways.*;
import static com.example.demo.payment.PaymentSignatures.*;

@Service
@RequiredArgsConstructor
public class ZaloPayGateway {
    private final PaymentConfig.Settings config;
    private final ObjectMapper json;
    private final HttpClient client = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10)).followRedirects(HttpClient.Redirect.NEVER).build();
    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("yyMMdd")
            .withZone(ZoneId.of("Asia/Ho_Chi_Minh"));
    private static final Set<String> SANDBOX_HOSTS = Set.of("qcgateway.zalopay.vn", "sbgateway.zalopay.vn", "sbcheckout.zalopay.vn");

    public record Lookup(Verified payment, boolean processing) {}
    public record Callback(String reference, Verified payment) {}
    public record Returned(String reference, BigDecimal amount) {}

    // Giữ nguyên khóa giao dịch nội bộ; mã gửi ZaloPay dài 39 ký tự và có ngày Việt Nam.
    public static String reference(Attempt attempt) {
        if (!attempt.id().matches("pay[a-f0-9]{32}")) throw invalid();
        return DATE.format(attempt.createdAt()) + "_" + attempt.id().substring(3);
    }

    public static String attemptId(String reference) {
        if (reference == null || !reference.matches("[0-9]{6}_[a-f0-9]{32}")) throw invalid();
        return "pay" + reference.substring(7);
    }

    public Link create(Attempt attempt) {
        requireConfigured();
        long seconds = Duration.between(Instant.now(), attempt.expiresAt()).getSeconds();
        if (seconds <= 0) throw new ApiException(HttpStatus.CONFLICT,
                "Mã thanh toán đã hết hạn. Vui lòng thử lại để cấp mã mới.");
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("app_id", Integer.parseInt(config.getZalopayAppId()));
        body.put("app_trans_id", reference(attempt));
        body.put("app_user", attempt.id());
        body.put("app_time", attempt.createdAt().toEpochMilli());
        body.put("expire_duration_seconds", 900);
        body.put("amount", attempt.amount().longValueExact());
        body.put("description", "GK Academy - Thanh toan hoc phi " + attempt.orderId());
        body.put("bank_code", "");
        body.put("item", "[]");
        try {
            body.put("embed_data", json.writeValueAsString(Map.of(
                    "redirecturl", config.zalopayRedirect(), "preferred_payment_method", new String[]{"domestic_card"})));
        } catch (Exception ex) {
            throw unavailable();
        }
        String callback = config.zalopayCallback();
        if (!callback.isBlank()) body.put("callback_url", callback);
        String input = String.join("|", body.get("app_id").toString(), body.get("app_trans_id").toString(),
                body.get("app_user").toString(), body.get("amount").toString(), body.get("app_time").toString(),
                body.get("embed_data").toString(), body.get("item").toString());
        body.put("mac", hmac("HmacSHA256", config.getZalopayKey1(), input));
        JsonNode value = post(config.zalopayCreate(), body);
        int code = value.path("return_code").asInt(-1);
        int detail = value.path("sub_return_code").asInt(0);
        // Trùng mã có thể do yêu cầu trước đã đến cổng nhưng phản hồi bị mất; không tạo mã mới.
        if (code == 2 && detail == -68) throw new DuplicateOrder();
        if (code == 2) throw new CreationRejected(detail);
        if (code != 1) throw unavailable();
        String url = value.path("order_url").asText("");
        try {
            URI uri = URI.create(url);
            if (!"https".equals(uri.getScheme()) || !SANDBOX_HOSTS.contains(uri.getHost())
                    || uri.getPort() != -1 || uri.getUserInfo() != null) throw new IllegalArgumentException();
        } catch (Exception ex) {
            throw unavailable();
        }
        return new Link(url, null);
    }

    public Verified query(Attempt attempt) {
        return lookup(attempt).payment();
    }

    public Lookup lookup(Attempt attempt) {
        requireConfigured();
        String ref = reference(attempt);
        String input = config.getZalopayAppId() + "|" + ref + "|" + config.getZalopayKey1();
        JsonNode value = post(config.zalopayQuery(), Map.of("app_id", Integer.parseInt(config.getZalopayAppId()),
                "app_trans_id", ref, "mac", hmac("HmacSHA256", config.getZalopayKey1(), input)));
        int code = value.path("return_code").asInt(-1);
        int detail = value.path("sub_return_code").asInt(0);
        if (code == 1) {
            return new Lookup(new Verified(attempt.id(), "ZALOPAY", money(value, "amount"), true, false,
                    transaction(value), "1"), false);
        }
        // Không coi lỗi cấu hình, thiếu số dư hoặc lỗi truy vấn là giao dịch đã hủy.
        if (code == 2 && Set.of(-54, -217, -332, -333).contains(detail)) {
            return new Lookup(new Verified(attempt.id(), "ZALOPAY", attempt.amount(), false, false, null,
                    Integer.toString(detail)), false);
        }
        if (code == 3 || code == 2 && detail == -63) {
            return new Lookup(new Verified(attempt.id(), "ZALOPAY", attempt.amount(), false, true, null, "3"),
                    !value.path("is_processing").isBoolean() || value.path("is_processing").asBoolean());
        }
        throw unavailable();
    }

    public Callback verifyCallback(JsonNode envelope) {
        requireConfigured();
        if (!envelope.path("data").isTextual() || envelope.path("type").asInt(-1) != 1) throw invalid();
        String raw = envelope.path("data").asText();
        if (raw.length() > 65536 || !equal(hmac("HmacSHA256", config.getZalopayKey2(), raw),
                envelope.path("mac").asText(""))) throw invalid();
        try {
            // Xác minh chuỗi gốc trước khi parse, tránh thay đổi dữ liệu được ký.
            JsonNode data = json.readTree(raw);
            if (!config.getZalopayAppId().equals(data.path("app_id").asText())) throw invalid();
            String ref = data.path("app_trans_id").asText();
            return new Callback(ref, new Verified(attemptId(ref), "ZALOPAY", money(data, "amount"),
                    true, false, transaction(data), "1"));
        } catch (ApiException ex) {
            throw ex;
        } catch (Exception ex) {
            throw invalid();
        }
    }

    public Returned verifyReturn(Map<String, String> params) {
        requireConfigured();
        String[] fields = {"appid", "apptransid", "pmcid", "bankcode", "amount", "discountamount", "status"};
        String[] values = new String[fields.length];
        for (int i = 0; i < fields.length; i++) {
            String value = params.get(fields[i]);
            if (value == null || value.length() > 100 || value.contains("|")) throw invalid();
            values[i] = value;
        }
        if (!config.getZalopayAppId().equals(params.get("appid"))
                || !equal(hmac("HmacSHA256", config.getZalopayKey2(), String.join("|", values)),
                params.get("checksum"))) throw invalid();
        String ref = params.get("apptransid");
        attemptId(ref);
        String amount = params.get("amount");
        if (!amount.matches("[0-9]{1,14}")) throw invalid();
        return new Returned(ref, new BigDecimal(amount));
    }

    private JsonNode post(String endpoint, Map<String, ?> body) {
        try {
            HttpRequest request = HttpRequest.newBuilder(URI.create(endpoint)).timeout(Duration.ofSeconds(15))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(json.writeValueAsString(body))).build();
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() != 200 || response.body().length() > 65536) throw unavailable();
            JsonNode value = json.readTree(response.body());
            if (value == null || !value.isObject()) throw unavailable();
            return value;
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw unavailable();
        } catch (Exception ex) {
            throw unavailable();
        }
    }

    private void requireConfigured() {
        if (!config.configured("ZALOPAY")) throw new ApiException(HttpStatus.SERVICE_UNAVAILABLE,
                "ZaloPay Sandbox chưa được cấu hình hợp lệ.");
    }

    private static BigDecimal money(JsonNode data, String field) {
        String amount = data.path(field).asText();
        if (!amount.matches("[0-9]{1,14}") || new BigDecimal(amount).signum() <= 0) throw invalid();
        return new BigDecimal(amount);
    }

    private static String transaction(JsonNode data) {
        String id = data.path("zp_trans_id").asText();
        if (!id.matches("[0-9]{1,20}") || id.matches("0+")) throw invalid();
        return id;
    }

    private static ApiException invalid() {
        return new ApiException(HttpStatus.BAD_REQUEST, "Chữ ký hoặc thông tin giao dịch ZaloPay không hợp lệ.");
    }

    private static ApiException unavailable() {
        return new ApiException(HttpStatus.BAD_GATEWAY,
                "Chưa đối chiếu được với ZaloPay. Đơn chưa được xác nhận thanh toán; vui lòng kiểm tra lại trong profile.");
    }

    public static class DuplicateOrder extends ApiException {
        public DuplicateOrder() {
            super(HttpStatus.CONFLICT,
                    "ZaloPay đang xử lý yêu cầu trước. Vui lòng kiểm tra trạng thái trong profile trước khi thử lại.");
        }
    }

    public static class CreationRejected extends ApiException {
        public CreationRejected(int code) {
            super(HttpStatus.BAD_GATEWAY, "ZaloPay chưa tạo được mã thanh toán (" + code
                    + "). Vui lòng kiểm tra cấu hình Sandbox hoặc thử lại.");
        }
    }
}

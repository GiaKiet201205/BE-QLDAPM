package com.example.demo.payment;

import com.example.demo.exception.ApiException;
import com.example.demo.service.ClassManagementService;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

import static com.example.demo.payment.PaymentDtos.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class PaymentService {
    private final PaymentConfig.Settings config;
    private final PaymentLedger ledger;
    private final PaymentGateways gateways;
    private final ZaloPayGateway zalopay;
    private final ClassManagementService classes;
    private final Set<String> creating = ConcurrentHashMap.newKeySet();
    private final Map<String, Instant> nextZalopayQuery = new ConcurrentHashMap<>();

    public Map<String, Provider> providers() {
        Map<String, Provider> result = new LinkedHashMap<>();
        for (String provider : List.of("VNPAY", "ZALOPAY")) {
            boolean enabled = config.configured(provider);
            String reason = "ZALOPAY".equals(provider) ? "ZaloPay chỉ hỗ trợ Sandbox và cần bộ AppID, Key1, Key2 hợp lệ."
                    : "Chưa cấu hình tài khoản thanh toán và địa chỉ nhận kết quả.";
            result.put(provider, new Provider(enabled, config.isSandbox(), enabled ? "" : reason));
        }
        return result;
    }

    public Status start(String username, String orderId, String provider, String ip) {
        if (!Set.of("VNPAY", "ZALOPAY").contains(provider) || !config.configured(provider))
            throw new ApiException(HttpStatus.SERVICE_UNAVAILABLE,
                    "Phương thức thanh toán chưa được cấu hình. Vui lòng chọn phương thức đang hoạt động hoặc liên hệ GK.");
        Attempt attempt = ledger.prepare(username, orderId, provider);
        for (int retry = 0; retry < 2 && "CREATING".equals(attempt.status()); retry++) {
            String attemptId = attempt.id();
            if (!creating.add(attemptId)) throw new ApiException(HttpStatus.CONFLICT,
                    "Đang tạo mã thanh toán. Vui lòng chờ trong giây lát.");
            try {
                var link = "ZALOPAY".equals(provider) ? zalopay.create(attempt) : gateways.create(attempt, ip);
                ledger.created(attemptId, link.url(), link.qr());
                break;
            } catch (ZaloPayGateway.DuplicateOrder ex) {
                if (retry > 0) throw ex;
                var lookup = zalopay.lookup(attempt);
                if (lookup.payment().success()) {
                    notify(lookup.payment());
                    return ledger.status(username, orderId, config.isSandbox());
                }
                if (lookup.processing()) throw ex;
                // Chỉ thay mã bị kẹt trước khi từng trả link cho FE, sau khi đã đối chiếu chưa trả tiền.
                // Giữ bản ghi cũ để vẫn ghi nhận được callback đến trễ.
                if (!ledger.retireUnopenedZalopay(attemptId))
                    return ledger.status(username, orderId, config.isSandbox());
                attempt = ledger.prepare(username, orderId, provider);
            } catch (ZaloPayGateway.CreationRejected ex) {
                ledger.failedCreation(attemptId);
                throw ex;
            } finally {
                creating.remove(attemptId);
            }
        }
        return ledger.status(username, orderId, config.isSandbox());
    }

    public Status status(String username, String orderId) {
        // Kiểm tra chủ đơn trước khi truy vấn nhà cung cấp hoặc thay đổi dữ liệu.
        Status status = ledger.status(username, orderId, config.isSandbox());
        if (status.attempt() != null && "ZALOPAY".equals(status.attempt().provider())
                && !"PAID".equals(status.paymentStatus()) && !"FAILED".equals(status.attempt().status())) {
            reconcileZalopay(status.attempt());
            return ledger.status(username, orderId, config.isSandbox());
        }
        return status;
    }

    public Receipt notify(PaymentGateways.Verified result) {
        Attempt attempt = ledger.find(result.attemptId());
        if (!attempt.provider().equals(result.provider()) || attempt.amount().compareTo(result.amount()) != 0)
            throw new ApiException(HttpStatus.BAD_REQUEST, "Số tiền hoặc nhà cung cấp không khớp.");
        if (result.pending()) return new Receipt(attempt.orderId(), true);
        Receipt receipt = ledger.receive(result.attemptId(), result.provider(), result.amount(), result.success(),
                result.transactionId(), result.code());
        if (result.success()) {
            try {
                classes.enrollPaidOrder(receipt.orderId());
            } catch (Exception ex) {
                ledger.fulfillmentIssue(receipt.orderId());
            }
        }
        return receipt;
    }

    public Receipt zalopayCallback(JsonNode body) {
        var callback = zalopay.verifyCallback(body);
        checkZalopayReference(callback.reference(), callback.payment().amount());
        return notify(callback.payment());
    }

    public String zalopayReturned(Map<String, String> params) {
        var returned = zalopay.verifyReturn(params);
        Attempt attempt = checkZalopayReference(returned.reference(), returned.amount());
        // URL đã được xác minh chữ ký: đối chiếu ngay, không chờ lịch nền một phút.
        // Chỉ ghi nhận thành công từ callback hoặc kết quả API query của ZaloPay.
        reconcileZalopay(attempt, true);
        return config.frontend() + "/user/profile?payment=" + attempt.orderId();
    }

    private Attempt checkZalopayReference(String reference, java.math.BigDecimal amount) {
        Attempt attempt = ledger.find(ZaloPayGateway.attemptId(reference));
        if (!"ZALOPAY".equals(attempt.provider()) || !reference.equals(ZaloPayGateway.reference(attempt))
                || attempt.amount().compareTo(amount) != 0)
            throw new ApiException(HttpStatus.BAD_REQUEST, "Thông tin giao dịch ZaloPay không khớp đơn đăng ký.");
        return attempt;
    }

    public void reconcilePendingZalopay() {
        if (!config.configured("ZALOPAY")) return;
        for (Attempt attempt : ledger.pendingZalopay()) {
            try {
                reconcileZalopay(attempt);
            } catch (Exception ex) {
                // Không ghi khóa hoặc nội dung phản hồi từ cổng thanh toán vào log.
                log.warn("Chưa cập nhật được giao dịch ZaloPay {}", attempt.id());
            }
        }
    }

    private void reconcileZalopay(Attempt attempt) {
        reconcileZalopay(attempt, false);
    }

    private void reconcileZalopay(Attempt attempt, boolean returnedFromGateway) {
        if ("PAID".equals(attempt.status()) || !config.configured("ZALOPAY")) return;
        Instant now = Instant.now();
        nextZalopayQuery.entrySet().removeIf(entry -> entry.getValue().isBefore(now.minusSeconds(3600)));
        Instant next = now.plusSeconds(60);
        if (returnedFromGateway) {
            nextZalopayQuery.put(attempt.id(), next);
        } else {
            Instant previous = nextZalopayQuery.putIfAbsent(attempt.id(), next);
            if (previous != null && (previous.isAfter(now) || !nextZalopayQuery.replace(attempt.id(), previous, next))) return;
        }
        PaymentGateways.Verified result;
        try {
            result = zalopay.query(attempt);
        } catch (ApiException ex) {
            log.warn("Chưa đối chiếu được giao dịch ZaloPay {}; sẽ thử lại", attempt.id());
            return;
        }
        if (!result.pending()) notify(result);
        else if (!attempt.expiresAt().isAfter(now)) ledger.expireZalopay(attempt.id());
    }

    public String returned(PaymentGateways.Verified result) {
        Attempt attempt = ledger.find(result.attemptId());
        if (!attempt.provider().equals(result.provider()) || attempt.amount().compareTo(result.amount()) != 0)
            throw new ApiException(HttpStatus.BAD_REQUEST, "Thông tin trả về không khớp.");
        // Chỉ Sandbox mới ghi nhận VNPAY từ Return URL đã được kiểm tra chữ ký tại controller.
        // Môi trường thật tiếp tục chờ IPN và dùng chung cơ chế chống xử lý trùng.
        if (config.isSandbox() && "VNPAY".equals(attempt.provider())) {
            if (result.success() && (result.transactionId() == null
                    || !result.transactionId().matches("[0-9]{1,15}") || result.transactionId().matches("0+")))
                throw new ApiException(HttpStatus.BAD_REQUEST, "Thiếu mã giao dịch VNPAY thành công hợp lệ.");
            notify(result);
        }
        return config.frontend() + "/user/profile?payment=" + attempt.orderId();
    }
}

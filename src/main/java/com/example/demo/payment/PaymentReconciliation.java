package com.example.demo.payment;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;

@Configuration
@EnableScheduling
@RequiredArgsConstructor
public class PaymentReconciliation {
    private final PaymentService payments;

    // Đối chiếu cả khi người dùng đóng trình duyệt hoặc localhost không nhận được callback.
    @Scheduled(initialDelay = 60000, fixedDelay = 60000)
    public void reconcileZalopay() {
        payments.reconcilePendingZalopay();
    }
}

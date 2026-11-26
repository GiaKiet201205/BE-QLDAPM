-- Bổ sung ZaloPay, giữ nguyên giao dịch MoMo cũ và dữ liệu đã thanh toán.
ALTER TABLE payment_attempts
 DROP CONSTRAINT payment_attempts_provider_check,
 ADD CONSTRAINT payment_attempts_provider_check CHECK (provider IN ('VNPAY','MOMO','ZALOPAY'));

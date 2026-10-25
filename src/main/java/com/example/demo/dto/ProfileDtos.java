package com.example.demo.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public class ProfileDtos {
    public record UpdateProfileRequest(
            @NotBlank(message = "Vui lòng nhập họ tên.") @Size(max = 100) String fullName,
            @NotBlank(message = "Vui lòng nhập email.") @Email @Size(max = 100) String email,
            @Size(max = 15) @Pattern(regexp = "\\+?[0-9]{9,15}|", message = "Số điện thoại phải có 9–15 chữ số.") String phone
    ) {}

    public record ChangePasswordRequest(
            @NotBlank(message = "Vui lòng nhập mật khẩu hiện tại.") @Size(max = 255) String currentPassword,
            @NotBlank @Size(min = 6, max = 72, message = "Mật khẩu mới phải có 6–72 ký tự.") String newPassword,
            @NotBlank(message = "Vui lòng xác nhận mật khẩu mới.") @Size(max = 72) String confirmPassword
    ) {}
}

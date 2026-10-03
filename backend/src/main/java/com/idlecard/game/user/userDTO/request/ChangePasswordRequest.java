package com.idlecard.game.user.userDTO.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ChangePasswordRequest(
        @NotBlank String id,
        @NotBlank String oldPassword,
        @NotBlank @Size(min = 4, max = 50, message = "비밀번호는 4자 이상이어야 합니다") String newPassword
) {
}

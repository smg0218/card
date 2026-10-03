package com.idlecard.game.user.userDTO.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record RegisterRequest(
        @NotBlank @Pattern(regexp = "^[a-zA-Z0-9_]{4,20}$", message = "아이디는 영문/숫자/밑줄 4~20자여야 합니다") String id,
        @NotBlank @Size(min = 4, max = 50, message = "비밀번호는 4자 이상이어야 합니다") String password,
        @NotBlank @Size(min = 2, max = 20, message = "닉네임은 2~20자여야 합니다") String nickname
) {
}

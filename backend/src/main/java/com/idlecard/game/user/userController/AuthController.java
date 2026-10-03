package com.idlecard.game.user.userController;

import com.idlecard.game.user.userDTO.request.*;
import com.idlecard.game.user.userDTO.response.*;
import com.idlecard.game.user.userService.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @GetMapping("/check-id")
    public AvailabilityResponse checkId(@RequestParam String id) {
        return new AvailabilityResponse(authService.isIdAvailable(id));
    }

    @GetMapping("/check-nickname")
    public AvailabilityResponse checkNickname(@RequestParam String nickname) {
        return new AvailabilityResponse(authService.isNicknameAvailable(nickname));
    }

    @PostMapping("/register")
    public AuthResponse register(@Valid @RequestBody RegisterRequest request) {
        return authService.register(request.id(), request.password(), request.nickname());
    }

    @PostMapping("/login")
    public AuthResponse login(@Valid @RequestBody LoginRequest request) {
        return authService.login(request.id(), request.password());
    }

    @PostMapping("/withdraw")
    public void withdraw(@Valid @RequestBody WithdrawRequest request) {
        authService.withdraw(request.id(), request.password());
    }

    @GetMapping("/users/{id}")
    public UserInfoResponse userInfo(@PathVariable String id) {
        return authService.getUserInfo(id);
    }

    @PostMapping("/change-password")
    public void changePassword(@Valid @RequestBody ChangePasswordRequest request) {
        authService.changePassword(request.id(), request.oldPassword(), request.newPassword());
    }
}

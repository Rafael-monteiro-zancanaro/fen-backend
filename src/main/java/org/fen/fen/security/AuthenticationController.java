package org.fen.fen.security;

import jakarta.validation.Valid;
import org.fen.fen.security.dto.AuthUserResponse;
import org.fen.fen.security.dto.LoginRequest;
import org.fen.fen.security.dto.LoginResponse;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class AuthenticationController {

    private final AuthenticationService authenticationService;

    public AuthenticationController(AuthenticationService authenticationService) {
        this.authenticationService = authenticationService;
    }

    @PostMapping("/api/auth/login")
    public LoginResponse login(@Valid @RequestBody LoginRequest request) {
        return authenticationService.login(request);
    }

    @GetMapping("/api/auth/me")
    public AuthUserResponse currentUser(Authentication authentication) {
        return authenticationService.currentUser(authentication);
    }
}

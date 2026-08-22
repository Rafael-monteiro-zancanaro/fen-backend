package org.fen.fen.security;

import org.fen.fen.security.dto.AuthUserResponse;
import org.fen.fen.security.dto.LoginRequest;
import org.fen.fen.security.dto.LoginResponse;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;

import java.time.Clock;

@Service
public class AuthenticationService {

    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;
    private final Clock clock;

    public AuthenticationService(
            AuthenticationManager authenticationManager,
            JwtService jwtService,
            Clock clock
    ) {
        this.authenticationManager = authenticationManager;
        this.jwtService = jwtService;
        this.clock = clock;
    }

    public LoginResponse login(LoginRequest request) {
        Authentication authentication = authenticationManager.authenticate(
                UsernamePasswordAuthenticationToken.unauthenticated(
                        request.email(),
                        request.senha()
                )
        );
        FenUserDetails principal = principal(authentication);
        IssuedToken issuedToken = jwtService.issue(principal, clock.instant());
        return new LoginResponse(
                issuedToken.token(),
                issuedToken.expiresAt(),
                response(principal)
        );
    }

    public AuthUserResponse currentUser(Authentication authentication) {
        return response(principal(authentication));
    }

    private FenUserDetails principal(Authentication authentication) {
        if (authentication.getPrincipal() instanceof FenUserDetails principal) {
            return principal;
        }
        throw new BadCredentialsException("Credenciais inválidas");
    }

    private AuthUserResponse response(FenUserDetails principal) {
        return new AuthUserResponse(
                principal.userId(),
                principal.getUsername(),
                principal.role()
        );
    }
}

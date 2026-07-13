package org.fen.fen.infra;

import org.springframework.data.domain.AuditorAware;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

import java.security.Principal;
import java.util.Optional;

public class AuditorAwareImpl implements AuditorAware<String> {

    private static final String SYSTEM = "Sistema";

    @Override
    public Optional<String> getCurrentAuditor() {
        return Optional.ofNullable(SecurityContextHolder.getContext().getAuthentication())
                .map(Principal::getName)
                .or(() -> Optional.of(SYSTEM));
    }
}

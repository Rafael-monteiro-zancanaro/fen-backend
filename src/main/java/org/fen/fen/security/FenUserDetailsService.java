package org.fen.fen.security;

import org.fen.fen.domain.Role;
import org.fen.fen.domain.SituacaoUsuario;
import org.fen.fen.domain.Usuario;
import org.fen.fen.repository.UsuarioRepository;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

@Service
public class FenUserDetailsService implements UserDetailsService {

    private final UsuarioRepository usuarioRepository;

    public FenUserDetailsService(UsuarioRepository usuarioRepository) {
        this.usuarioRepository = usuarioRepository;
    }

    @Override
    public UserDetails loadUserByUsername(String email) {
        return usuarioRepository.findByEmailIgnoreCase(email)
                .map(FenUserDetails::from)
                .orElseThrow(() -> new UsernameNotFoundException("Credenciais inválidas"));
    }

    FenUserDetails loadUserById(UUID userId) {
        return usuarioRepository.findById(userId)
                .map(FenUserDetails::from)
                .orElseThrow(() -> new UsernameNotFoundException("Credenciais inválidas"));
    }
}

record FenUserDetails(
        UUID userId,
        String username,
        String password,
        Role role,
        boolean enabled
) implements UserDetails {

    static FenUserDetails from(Usuario usuario) {
        return new FenUserDetails(
                usuario.getId(),
                usuario.getEmail(),
                usuario.getPasswordHash(),
                usuario.getRole(),
                usuario.getSituacao() == SituacaoUsuario.ATIVO
        );
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of(new SimpleGrantedAuthority("ROLE_" + role.name()));
    }

    @Override
    public String getPassword() {
        return password;
    }

    @Override
    public String getUsername() {
        return username;
    }

    @Override
    public boolean isEnabled() {
        return enabled;
    }
}

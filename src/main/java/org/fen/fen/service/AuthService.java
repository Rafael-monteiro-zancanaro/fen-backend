package org.fen.fen.service;

import lombok.RequiredArgsConstructor;
import org.fen.fen.domain.Usuario;
import org.fen.fen.dto.request.LoginDTO;
import org.fen.fen.infra.JwtUtil;
import org.fen.fen.repository.UsuarioRepository;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

import static org.fen.fen.util.Formatters.f;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;

    @Transactional(readOnly = true)
    public String login(LoginDTO loginDTO) {
        Usuario usuario = this.usuarioRepository.findByEmail(loginDTO.email())
                .orElseThrow(() -> new UsernameNotFoundException(f("Usuário não encontrado %s", loginDTO.email())));

        if (passwordEncoder.matches(loginDTO.password(), usuario.getHashSenha())) {
            return jwtUtil.generateToken(usuario.getEmail());
        }

        throw new BadCredentialsException(f("A senha informada está incorreta. Tente novamente"));
    }






}

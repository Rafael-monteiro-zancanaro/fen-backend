package org.fen.fen.service;

import lombok.RequiredArgsConstructor;
import org.fen.fen.domain.SolicitacaoRecuperacaoSenha;
import org.fen.fen.domain.StatusSolicitacaoRecuperacaoSenha;
import org.fen.fen.domain.Usuario;
import org.fen.fen.recuperacaosenha.dto.SolicitacaoRecuperacaoSenhaRequest;
import org.fen.fen.recuperacaosenha.dto.SolicitacaoRecuperacaoSenhaResponse;
import org.fen.fen.repository.SolicitacaoRecuperacaoSenhaRepository;
import org.fen.fen.repository.UsuarioRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Locale;
import java.util.NoSuchElementException;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class SolicitacaoRecuperacaoSenhaService {
    private final SolicitacaoRecuperacaoSenhaRepository repository;
    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;

    @Transactional
    public SolicitacaoRecuperacaoSenhaResponse criar(SolicitacaoRecuperacaoSenhaRequest request) {
        Usuario usuario = usuarioRepository.findByEmailIgnoreCase(request.email().trim())
                .orElseThrow(() -> new NoSuchElementException("Usuário não encontrado"));
        return resposta(repository.saveAndFlush(new SolicitacaoRecuperacaoSenha(usuario,
                passwordEncoder.encode(request.novaSenha()))));
    }

    @Transactional(readOnly = true)
    public Page<SolicitacaoRecuperacaoSenhaResponse> listar(String email,
            StatusSolicitacaoRecuperacaoSenha status, Pageable pageable) {
        return repository.buscar(email == null ? "" : email.trim(), status, pageable).map(this::resposta);
    }

    @Transactional(readOnly = true)
    public SolicitacaoRecuperacaoSenhaResponse buscar(UUID id) { return resposta(encontrar(id)); }

    @Transactional
    public SolicitacaoRecuperacaoSenhaResponse aprovar(UUID id) {
        SolicitacaoRecuperacaoSenha solicitacao = encontrar(id);
        validarPendente(solicitacao);
        solicitacao.getUsuario().setPasswordHash(solicitacao.getNovaSenhaHash());
        solicitacao.setStatus(StatusSolicitacaoRecuperacaoSenha.APROVADA);
        return resposta(repository.saveAndFlush(solicitacao));
    }

    @Transactional
    public SolicitacaoRecuperacaoSenhaResponse rejeitar(UUID id) {
        SolicitacaoRecuperacaoSenha solicitacao = encontrar(id);
        validarPendente(solicitacao);
        solicitacao.setStatus(StatusSolicitacaoRecuperacaoSenha.REJEITADA);
        return resposta(repository.saveAndFlush(solicitacao));
    }

    private SolicitacaoRecuperacaoSenha encontrar(UUID id) {
        return repository.buscarDetalhe(id).orElseThrow(() -> new NoSuchElementException("Solicitação não encontrada"));
    }
    private void validarPendente(SolicitacaoRecuperacaoSenha s) {
        if (s.getStatus() != StatusSolicitacaoRecuperacaoSenha.PENDENTE) {
            throw new IllegalStateException("Solicitação já foi processada");
        }
    }
    private SolicitacaoRecuperacaoSenhaResponse resposta(SolicitacaoRecuperacaoSenha s) {
        return new SolicitacaoRecuperacaoSenhaResponse(s.getId(), s.getUsuario().getEmail(), s.getStatus(), s.getCreatedAt());
    }
}

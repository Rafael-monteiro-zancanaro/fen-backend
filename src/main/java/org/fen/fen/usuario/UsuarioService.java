package org.fen.fen.usuario;

import org.fen.fen.error.BusinessRuleException;
import org.fen.fen.error.ConflictException;
import org.fen.fen.usuario.dto.UsuarioRegisterRequest;
import org.fen.fen.usuario.dto.UsuarioRegisterResponse;
import org.fen.fen.usuario.dto.SupervisorResponse;
import org.fen.fen.usuario.dto.UsuarioPendenteDetailResponse;
import org.fen.fen.usuario.dto.UsuarioPendenteSummaryResponse;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Locale;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.UUID;

@Service
public class UsuarioService {

    private static final Set<Role> PUBLIC_REGISTRATION_ROLES = Set.of(
            Role.FARMACEUTICO,
            Role.ESTAGIARIO
    );

    private final UsuarioRepository usuarioRepository;
    private final FuncionarioRepository funcionarioRepository;
    private final PasswordEncoder passwordEncoder;
    private final UsuarioMapper usuarioMapper;

    public UsuarioService(
            UsuarioRepository usuarioRepository,
            FuncionarioRepository funcionarioRepository,
            PasswordEncoder passwordEncoder,
            UsuarioMapper usuarioMapper
    ) {
        this.usuarioRepository = usuarioRepository;
        this.funcionarioRepository = funcionarioRepository;
        this.passwordEncoder = passwordEncoder;
        this.usuarioMapper = usuarioMapper;
    }

    @Transactional
    public UsuarioRegisterResponse register(UsuarioRegisterRequest request) {
        validateRoleSpecificFields(request);
        Funcionario supervisor = resolveSupervisor(request);

        String normalizedEmail = request.email().trim().toLowerCase(Locale.ROOT);
        String normalizedCpf = request.cpf().replaceAll("\\D", "");
        validateUniqueness(normalizedEmail, normalizedCpf);

        Usuario usuario = usuarioMapper.toUsuario(
                request,
                normalizedEmail,
                passwordEncoder.encode(request.senha())
        );
        Usuario savedUsuario = usuarioRepository.save(usuario);

        Funcionario funcionario = usuarioMapper.toFuncionario(request, savedUsuario, supervisor);
        Funcionario savedFuncionario = funcionarioRepository.save(funcionario);

        return usuarioMapper.toRegisterResponse(savedUsuario, savedFuncionario);
    }

    @Transactional(readOnly = true)
    public List<SupervisorResponse> findSupervisoresAtivos() {
        return funcionarioRepository.findSupervisoresAtivos().stream()
                .map(usuarioMapper::toSupervisorResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<UsuarioPendenteSummaryResponse> findPendentes() {
        return funcionarioRepository
                .findAllByUsuarioSituacaoOrderByUsuarioCreatedAtAsc(SituacaoUsuario.PENDENTE)
                .stream()
                .map(funcionario -> usuarioMapper.toPendenteSummary(
                        funcionario.getUsuario(),
                        funcionario
                ))
                .toList();
    }

    @Transactional(readOnly = true)
    public UsuarioPendenteDetailResponse findPendente(UUID id) {
        Usuario usuario = findPendingUsuario(id);
        return usuarioMapper.toPendenteDetail(usuario, findFuncionario(id));
    }

    @Transactional
    public void aprovar(UUID id) {
        Usuario usuario = findPendingUsuario(id);
        usuario.setSituacao(SituacaoUsuario.ATIVO);
        usuarioRepository.save(usuario);
    }

    @Transactional
    public void rejeitar(UUID id) {
        Usuario usuario = findPendingUsuario(id);
        Funcionario funcionario = findFuncionario(id);
        funcionarioRepository.delete(funcionario);
        funcionarioRepository.flush();
        usuarioRepository.delete(usuario);
    }

    private void validateRoleSpecificFields(UsuarioRegisterRequest request) {
        if (!PUBLIC_REGISTRATION_ROLES.contains(request.role())) {
            throw new BusinessRuleException("Role não permitida para cadastro público");
        }
        if (request.role() == Role.FARMACEUTICO
                && (request.crf() == null || request.crf().isBlank())) {
            throw new BusinessRuleException("CRF é obrigatório para farmacêutico");
        }
        if (request.role() == Role.FARMACEUTICO && request.responsavelTecnico() == null) {
            throw new BusinessRuleException(
                    "Indicação de responsável técnico é obrigatória para farmacêutico"
            );
        }
        if (request.role() == Role.ESTAGIARIO && request.supervisorId() == null) {
            throw new BusinessRuleException("Supervisor é obrigatório para estagiário");
        }
        if (request.role() == Role.ESTAGIARIO && request.tipoEstagio() == null) {
            throw new BusinessRuleException("Tipo de estágio é obrigatório para estagiário");
        }
        if (request.role() == Role.ESTAGIARIO && request.inicioVigencia() == null) {
            throw new BusinessRuleException("Início da vigência é obrigatório para estagiário");
        }
        if (request.role() == Role.ESTAGIARIO && request.fimVigencia() == null) {
            throw new BusinessRuleException("Fim da vigência é obrigatório para estagiário");
        }
        if (request.role() == Role.ESTAGIARIO
                && request.fimVigencia().isBefore(request.inicioVigencia())) {
            throw new BusinessRuleException("Fim da vigência deve ser igual ou posterior ao início");
        }
    }

    private Funcionario resolveSupervisor(UsuarioRegisterRequest request) {
        if (request.role() != Role.ESTAGIARIO) {
            return null;
        }

        Funcionario supervisor = funcionarioRepository.findById(request.supervisorId())
                .orElseThrow(() -> new BusinessRuleException("Supervisor inválido"));
        Role supervisorRole = supervisor.getUsuario().getRole();
        if (supervisor.getUsuario().getSituacao() != SituacaoUsuario.ATIVO
                || (supervisorRole != Role.ADMIN && supervisorRole != Role.FARMACEUTICO)) {
            throw new BusinessRuleException("Supervisor inválido");
        }
        return supervisor;
    }

    private void validateUniqueness(String normalizedEmail, String normalizedCpf) {
        if (usuarioRepository.existsByEmailIgnoreCase(normalizedEmail)) {
            throw new ConflictException("E-mail já cadastrado");
        }
        if (funcionarioRepository.existsByCpf(normalizedCpf)) {
            throw new ConflictException("CPF já cadastrado");
        }
    }

    private Usuario findPendingUsuario(UUID id) {
        Usuario usuario = usuarioRepository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("Usuário não encontrado"));
        if (usuario.getSituacao() != SituacaoUsuario.PENDENTE) {
            throw new BusinessRuleException("Usuário não possui cadastro pendente");
        }
        return usuario;
    }

    private Funcionario findFuncionario(UUID usuarioId) {
        return funcionarioRepository.findByUsuarioId(usuarioId)
                .orElseThrow(() -> new NoSuchElementException("Funcionário não encontrado"));
    }
}

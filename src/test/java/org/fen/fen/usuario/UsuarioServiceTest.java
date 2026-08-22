package org.fen.fen.usuario;

import org.fen.fen.error.BusinessRuleException;
import org.fen.fen.error.ConflictException;
import org.fen.fen.usuario.dto.UsuarioRegisterRequest;
import org.fen.fen.usuario.dto.UsuarioRegisterResponse;
import org.fen.fen.usuario.dto.SupervisorResponse;
import org.fen.fen.usuario.dto.UsuarioPendenteDetailResponse;
import org.fen.fen.usuario.dto.UsuarioPendenteSummaryResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UsuarioServiceTest {

    private static final UUID USUARIO_ID = UUID.fromString("00000000-0000-0000-0000-000000000201");
    private static final UUID FUNCIONARIO_ID = UUID.fromString("00000000-0000-0000-0000-000000000202");

    @Mock
    private UsuarioRepository usuarioRepository;

    @Mock
    private FuncionarioRepository funcionarioRepository;

    private final PasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    private UsuarioService usuarioService;
    private Usuario savedUsuario;
    private Funcionario savedFuncionario;

    @BeforeEach
    void setUp() {
        lenient().when(usuarioRepository.save(any(Usuario.class))).thenAnswer(invocation -> {
            savedUsuario = invocation.getArgument(0);
            savedUsuario.setId(USUARIO_ID);
            return savedUsuario;
        });
        lenient().when(funcionarioRepository.save(any(Funcionario.class))).thenAnswer(invocation -> {
            savedFuncionario = invocation.getArgument(0);
            savedFuncionario.setId(FUNCIONARIO_ID);
            return savedFuncionario;
        });

        usuarioService = new UsuarioService(
                usuarioRepository,
                funcionarioRepository,
                passwordEncoder,
                new UsuarioMapper()
        );
    }

    @Test
    void registersPendingPharmacistWithNormalizedCredentialsAndProfile() {
        UsuarioRegisterResponse response = usuarioService.register(pharmacistRequest());

        assertThat(savedUsuario.getEmail()).isEqualTo("nova@fen.br");
        assertThat(savedUsuario.getPasswordHash()).isNotEqualTo("segredo123");
        assertThat(passwordEncoder.matches("segredo123", savedUsuario.getPasswordHash())).isTrue();
        assertThat(savedUsuario.getRole()).isEqualTo(Role.FARMACEUTICO);
        assertThat(savedUsuario.getSituacao()).isEqualTo(SituacaoUsuario.PENDENTE);
        assertThat(savedFuncionario.getUsuario()).isSameAs(savedUsuario);
        assertThat(savedFuncionario.getNome()).isEqualTo("Nova Farmacêutica");
        assertThat(savedFuncionario.getCpf()).isEqualTo("12345678901");
        assertThat(savedFuncionario.getCrf()).isEqualTo("PR-12345");
        assertThat(savedFuncionario.getResponsavelTecnico()).isTrue();
        assertThat(response.usuarioId()).isEqualTo(USUARIO_ID);
        assertThat(response.funcionarioId()).isEqualTo(FUNCIONARIO_ID);
        assertThat(response.situacao()).isEqualTo(SituacaoUsuario.PENDENTE);
    }

    @Test
    void rejectsAdminRegistration() {
        UsuarioRegisterRequest request = pharmacistRequest(Role.ADMIN);

        assertThatThrownBy(() -> usuarioService.register(request))
                .isInstanceOf(BusinessRuleException.class);
        assertThat(savedUsuario).isNull();
        assertThat(savedFuncionario).isNull();
    }

    @Test
    void rejectsDuplicateNormalizedEmail() {
        when(usuarioRepository.existsByEmailIgnoreCase("nova@fen.br")).thenReturn(true);

        assertThatThrownBy(() -> usuarioService.register(pharmacistRequest()))
                .isInstanceOf(ConflictException.class);
        assertThat(savedUsuario).isNull();
        assertThat(savedFuncionario).isNull();
    }

    @Test
    void rejectsDuplicateNormalizedCpf() {
        when(funcionarioRepository.existsByCpf("12345678901")).thenReturn(true);

        assertThatThrownBy(() -> usuarioService.register(pharmacistRequest()))
                .isInstanceOf(ConflictException.class);
        assertThat(savedUsuario).isNull();
        assertThat(savedFuncionario).isNull();
    }

    @Test
    void requiresCrfForPharmacist() {
        UsuarioRegisterRequest request = pharmacistRequest(Role.FARMACEUTICO, null);

        assertThatThrownBy(() -> usuarioService.register(request))
                .isInstanceOf(BusinessRuleException.class);
        assertThat(savedUsuario).isNull();
        assertThat(savedFuncionario).isNull();
    }

    @Test
    void requiresTechnicalManagerChoiceForPharmacist() {
        UsuarioRegisterRequest valid = pharmacistRequest();
        UsuarioRegisterRequest request = new UsuarioRegisterRequest(
                valid.nome(), valid.cpf(), valid.dataNascimento(), valid.email(), valid.senha(), valid.role(),
                valid.crf(), null, valid.tipoEstagio(), valid.supervisorId(),
                valid.inicioVigencia(), valid.fimVigencia()
        );

        assertThatThrownBy(() -> usuarioService.register(request))
                .isInstanceOf(BusinessRuleException.class);
        assertThat(savedUsuario).isNull();
        assertThat(savedFuncionario).isNull();
    }

    @Test
    void acceptsExplicitFalseTechnicalManagerChoiceForPharmacist() {
        UsuarioRegisterRequest valid = pharmacistRequest();
        UsuarioRegisterRequest request = new UsuarioRegisterRequest(
                valid.nome(), valid.cpf(), valid.dataNascimento(), valid.email(), valid.senha(), valid.role(),
                valid.crf(), false, valid.tipoEstagio(), valid.supervisorId(),
                valid.inicioVigencia(), valid.fimVigencia()
        );

        usuarioService.register(request);

        assertThat(savedFuncionario.getResponsavelTecnico()).isFalse();
    }

    @Test
    void requiresSupervisorForIntern() {
        UsuarioRegisterRequest request = internRequest(
                null,
                LocalDate.of(2026, 8, 1),
                LocalDate.of(2026, 12, 1)
        );

        assertThatThrownBy(() -> usuarioService.register(request))
                .isInstanceOf(BusinessRuleException.class);
        assertThat(savedUsuario).isNull();
        assertThat(savedFuncionario).isNull();
    }

    @Test
    void requiresInternshipTypeForIntern() {
        UUID supervisorId = UUID.fromString("00000000-0000-0000-0000-000000000203");
        UsuarioRegisterRequest valid = internRequest(
                supervisorId,
                LocalDate.of(2026, 8, 1),
                LocalDate.of(2026, 12, 1)
        );
        UsuarioRegisterRequest request = new UsuarioRegisterRequest(
                valid.nome(), valid.cpf(), valid.dataNascimento(), valid.email(), valid.senha(), valid.role(),
                valid.crf(), valid.responsavelTecnico(), null, valid.supervisorId(),
                valid.inicioVigencia(), valid.fimVigencia()
        );

        assertThatThrownBy(() -> usuarioService.register(request))
                .isInstanceOf(BusinessRuleException.class);
        assertThat(savedUsuario).isNull();
        assertThat(savedFuncionario).isNull();
    }

    @Test
    void requiresStartOfVigencyForIntern() {
        UUID supervisorId = UUID.fromString("00000000-0000-0000-0000-000000000203");
        UsuarioRegisterRequest request = internRequest(
                supervisorId,
                null,
                LocalDate.of(2026, 12, 1)
        );

        assertThatThrownBy(() -> usuarioService.register(request))
                .isInstanceOf(BusinessRuleException.class);
        assertThat(savedUsuario).isNull();
        assertThat(savedFuncionario).isNull();
    }

    @Test
    void requiresEndOfVigencyForIntern() {
        UUID supervisorId = UUID.fromString("00000000-0000-0000-0000-000000000203");
        UsuarioRegisterRequest request = internRequest(
                supervisorId,
                LocalDate.of(2026, 8, 1),
                null
        );

        assertThatThrownBy(() -> usuarioService.register(request))
                .isInstanceOf(BusinessRuleException.class);
        assertThat(savedUsuario).isNull();
        assertThat(savedFuncionario).isNull();
    }

    @Test
    void rejectsInternVigencyEndingBeforeItStarts() {
        Funcionario supervisor = supervisor(Role.FARMACEUTICO, SituacaoUsuario.ATIVO);
        UsuarioRegisterRequest request = internRequest(
                supervisor.getId(),
                LocalDate.of(2026, 12, 1),
                LocalDate.of(2026, 8, 1)
        );

        assertThatThrownBy(() -> usuarioService.register(request))
                .isInstanceOf(BusinessRuleException.class);
        assertThat(savedUsuario).isNull();
        assertThat(savedFuncionario).isNull();
    }

    @Test
    void rejectsUnknownSupervisorForIntern() {
        UUID supervisorId = UUID.fromString("00000000-0000-0000-0000-000000000203");
        UsuarioRegisterRequest request = internRequest(
                supervisorId,
                LocalDate.of(2026, 8, 1),
                LocalDate.of(2026, 12, 1)
        );

        assertThatThrownBy(() -> usuarioService.register(request))
                .isInstanceOf(BusinessRuleException.class);
        assertThat(savedUsuario).isNull();
        assertThat(savedFuncionario).isNull();
    }

    @Test
    void rejectsPendingSupervisorForIntern() {
        Funcionario supervisor = supervisor(Role.FARMACEUTICO, SituacaoUsuario.PENDENTE);
        when(funcionarioRepository.findById(supervisor.getId())).thenReturn(Optional.of(supervisor));
        UsuarioRegisterRequest request = internRequest(
                supervisor.getId(),
                LocalDate.of(2026, 8, 1),
                LocalDate.of(2026, 12, 1)
        );

        assertThatThrownBy(() -> usuarioService.register(request))
                .isInstanceOf(BusinessRuleException.class);
        assertThat(savedUsuario).isNull();
        assertThat(savedFuncionario).isNull();
    }

    @Test
    void rejectsInternAsSupervisor() {
        Funcionario supervisor = supervisor(Role.ESTAGIARIO, SituacaoUsuario.ATIVO);
        when(funcionarioRepository.findById(supervisor.getId())).thenReturn(Optional.of(supervisor));
        UsuarioRegisterRequest request = internRequest(
                supervisor.getId(),
                LocalDate.of(2026, 8, 1),
                LocalDate.of(2026, 12, 1)
        );

        assertThatThrownBy(() -> usuarioService.register(request))
                .isInstanceOf(BusinessRuleException.class);
        assertThat(savedUsuario).isNull();
        assertThat(savedFuncionario).isNull();
    }

    @Test
    void registersInternWithEligibleSupervisorAndVigency() {
        Funcionario supervisor = supervisor(Role.ADMIN, SituacaoUsuario.ATIVO);
        when(funcionarioRepository.findById(supervisor.getId())).thenReturn(Optional.of(supervisor));
        UsuarioRegisterRequest request = internRequest(
                supervisor.getId(),
                LocalDate.of(2026, 8, 1),
                LocalDate.of(2026, 12, 1)
        );

        usuarioService.register(request);

        assertThat(savedFuncionario.getSupervisor()).isSameAs(supervisor);
        assertThat(savedFuncionario.getTipoEstagio()).isEqualTo(TipoEstagio.OBRIGATORIO);
        assertThat(savedFuncionario.getInicioVigencia()).isEqualTo(LocalDate.of(2026, 8, 1));
        assertThat(savedFuncionario.getFimVigencia()).isEqualTo(LocalDate.of(2026, 12, 1));
        assertThat(savedFuncionario.getCrf()).isNull();
        assertThat(savedFuncionario.getResponsavelTecnico()).isNull();
    }

    @Test
    void returnsMinimalSupervisorResponsesInRepositoryOrder() {
        Funcionario ana = supervisor(Role.FARMACEUTICO, SituacaoUsuario.ATIVO);
        ana.setNome("Ana");
        Funcionario bia = supervisor(Role.ADMIN, SituacaoUsuario.ATIVO);
        bia.setId(UUID.fromString("00000000-0000-0000-0000-000000000204"));
        bia.setNome("Bia");
        when(funcionarioRepository.findSupervisoresAtivos()).thenReturn(List.of(ana, bia));

        List<SupervisorResponse> responses = usuarioService.findSupervisoresAtivos();

        assertThat(responses).containsExactly(
                new SupervisorResponse(ana.getId(), "Ana"),
                new SupervisorResponse(bia.getId(), "Bia")
        );
    }

    @Test
    void createsUsuarioAndFuncionarioWithinOneTransaction() throws NoSuchMethodException {
        Transactional transactional = UsuarioService.class
                .getMethod("register", UsuarioRegisterRequest.class)
                .getAnnotation(Transactional.class);

        assertThat(transactional).isNotNull();
        assertThat(transactional.readOnly()).isFalse();
    }

    @Test
    void listsPendingRegistrationsInCreationOrderWithoutCredentials() {
        Usuario first = usuario(
                "primeira@fen.br",
                Role.FARMACEUTICO,
                SituacaoUsuario.PENDENTE,
                LocalDateTime.of(2026, 8, 20, 9, 0)
        );
        first.setId(UUID.fromString("00000000-0000-0000-0000-000000000211"));
        Usuario second = usuario(
                "segunda@fen.br",
                Role.ESTAGIARIO,
                SituacaoUsuario.PENDENTE,
                LocalDateTime.of(2026, 8, 21, 10, 0)
        );
        second.setId(UUID.fromString("00000000-0000-0000-0000-000000000212"));
        Funcionario firstProfile = funcionario(first, "Primeira", "11122233344");
        Funcionario secondProfile = funcionario(second, "Segunda", "55566677788");
        when(usuarioRepository.findAllBySituacaoOrderByCreatedAtAsc(SituacaoUsuario.PENDENTE))
                .thenReturn(List.of(first, second));
        when(funcionarioRepository.findByUsuarioId(first.getId())).thenReturn(Optional.of(firstProfile));
        when(funcionarioRepository.findByUsuarioId(second.getId())).thenReturn(Optional.of(secondProfile));

        List<UsuarioPendenteSummaryResponse> responses = usuarioService.findPendentes();

        assertThat(responses).containsExactly(
                new UsuarioPendenteSummaryResponse(
                        first.getId(), "Primeira", "primeira@fen.br", "11122233344",
                        Role.FARMACEUTICO, LocalDateTime.of(2026, 8, 20, 9, 0)
                ),
                new UsuarioPendenteSummaryResponse(
                        second.getId(), "Segunda", "segunda@fen.br", "55566677788",
                        Role.ESTAGIARIO, LocalDateTime.of(2026, 8, 21, 10, 0)
                )
        );
    }

    @Test
    void returnsFullPendingPharmacistDetailWithoutCredentials() {
        Usuario usuario = usuario(
                "farmaceutica@fen.br",
                Role.FARMACEUTICO,
                SituacaoUsuario.PENDENTE,
                LocalDateTime.of(2026, 8, 21, 11, 0)
        );
        Funcionario funcionario = funcionario(usuario, "Ana Farmacêutica", "12345678901");
        funcionario.setDataNascimento(LocalDate.of(1990, 1, 2));
        funcionario.setCrf("PR-54321");
        funcionario.setResponsavelTecnico(true);
        when(usuarioRepository.findById(usuario.getId())).thenReturn(Optional.of(usuario));
        when(funcionarioRepository.findByUsuarioId(usuario.getId())).thenReturn(Optional.of(funcionario));

        UsuarioPendenteDetailResponse response = usuarioService.findPendente(usuario.getId());

        assertThat(response).isEqualTo(new UsuarioPendenteDetailResponse(
                usuario.getId(), funcionario.getId(), "Ana Farmacêutica", "farmaceutica@fen.br",
                "12345678901", LocalDate.of(1990, 1, 2), Role.FARMACEUTICO,
                SituacaoUsuario.PENDENTE, LocalDateTime.of(2026, 8, 21, 11, 0),
                "PR-54321", true, null, null, null, null
        ));
    }

    @Test
    void returnsFullPendingInternDetailWithSupervisor() {
        Usuario usuario = usuario(
                "estagiaria@fen.br",
                Role.ESTAGIARIO,
                SituacaoUsuario.PENDENTE,
                LocalDateTime.of(2026, 8, 21, 12, 0)
        );
        Funcionario funcionario = funcionario(usuario, "Bia Estagiária", "98765432100");
        funcionario.setDataNascimento(LocalDate.of(2002, 2, 3));
        funcionario.setTipoEstagio(TipoEstagio.NAO_OBRIGATORIO);
        funcionario.setInicioVigencia(LocalDate.of(2026, 8, 1));
        funcionario.setFimVigencia(LocalDate.of(2026, 12, 15));
        Funcionario supervisor = supervisor(Role.FARMACEUTICO, SituacaoUsuario.ATIVO);
        supervisor.setNome("Carla Supervisora");
        funcionario.setSupervisor(supervisor);
        when(usuarioRepository.findById(usuario.getId())).thenReturn(Optional.of(usuario));
        when(funcionarioRepository.findByUsuarioId(usuario.getId())).thenReturn(Optional.of(funcionario));

        UsuarioPendenteDetailResponse response = usuarioService.findPendente(usuario.getId());

        assertThat(response).isEqualTo(new UsuarioPendenteDetailResponse(
                usuario.getId(), funcionario.getId(), "Bia Estagiária", "estagiaria@fen.br",
                "98765432100", LocalDate.of(2002, 2, 3), Role.ESTAGIARIO,
                SituacaoUsuario.PENDENTE, LocalDateTime.of(2026, 8, 21, 12, 0),
                null, null, TipoEstagio.NAO_OBRIGATORIO,
                new SupervisorResponse(supervisor.getId(), "Carla Supervisora"),
                LocalDate.of(2026, 8, 1), LocalDate.of(2026, 12, 15)
        ));
    }

    @Test
    void rejectsDetailForActiveUser() {
        Usuario usuario = usuario(
                "ativa@fen.br",
                Role.FARMACEUTICO,
                SituacaoUsuario.ATIVO,
                LocalDateTime.of(2026, 8, 21, 13, 0)
        );
        when(usuarioRepository.findById(usuario.getId())).thenReturn(Optional.of(usuario));

        assertThatThrownBy(() -> usuarioService.findPendente(usuario.getId()))
                .isInstanceOf(BusinessRuleException.class);
        verify(funcionarioRepository, never()).findByUsuarioId(any());
    }

    @Test
    void reportsMissingPendingUser() {
        UUID missingId = UUID.fromString("00000000-0000-0000-0000-000000000299");

        assertThatThrownBy(() -> usuarioService.findPendente(missingId))
                .isInstanceOf(NoSuchElementException.class);
    }

    @Test
    void approvesPendingUserWithinTransaction() throws NoSuchMethodException {
        Usuario usuario = usuario(
                "aprovar@fen.br",
                Role.FARMACEUTICO,
                SituacaoUsuario.PENDENTE,
                LocalDateTime.of(2026, 8, 21, 14, 0)
        );
        when(usuarioRepository.findById(usuario.getId())).thenReturn(Optional.of(usuario));

        usuarioService.aprovar(usuario.getId());

        assertThat(usuario.getSituacao()).isEqualTo(SituacaoUsuario.ATIVO);
        verify(usuarioRepository).save(usuario);
        Transactional transactional = UsuarioService.class
                .getMethod("aprovar", UUID.class)
                .getAnnotation(Transactional.class);
        assertThat(transactional).isNotNull();
        assertThat(transactional.readOnly()).isFalse();
    }

    @Test
    void refusesToApproveActiveUser() {
        Usuario usuario = usuario(
                "ativa@fen.br",
                Role.FARMACEUTICO,
                SituacaoUsuario.ATIVO,
                LocalDateTime.of(2026, 8, 21, 15, 0)
        );
        when(usuarioRepository.findById(usuario.getId())).thenReturn(Optional.of(usuario));

        assertThatThrownBy(() -> usuarioService.aprovar(usuario.getId()))
                .isInstanceOf(BusinessRuleException.class);
        verify(usuarioRepository, never()).save(any());
    }

    @Test
    void rejectsPendingUserByDeletingProfileBeforeCredentialsWithinTransaction()
            throws NoSuchMethodException {
        Usuario usuario = usuario(
                "rejeitar@fen.br",
                Role.FARMACEUTICO,
                SituacaoUsuario.PENDENTE,
                LocalDateTime.of(2026, 8, 21, 16, 0)
        );
        Funcionario funcionario = funcionario(usuario, "Rejeitada", "11133355577");
        when(usuarioRepository.findById(usuario.getId())).thenReturn(Optional.of(usuario));
        when(funcionarioRepository.findByUsuarioId(usuario.getId())).thenReturn(Optional.of(funcionario));

        usuarioService.rejeitar(usuario.getId());

        var deletionOrder = inOrder(funcionarioRepository, usuarioRepository);
        deletionOrder.verify(funcionarioRepository).delete(funcionario);
        deletionOrder.verify(funcionarioRepository).flush();
        deletionOrder.verify(usuarioRepository).delete(usuario);
        Transactional transactional = UsuarioService.class
                .getMethod("rejeitar", UUID.class)
                .getAnnotation(Transactional.class);
        assertThat(transactional).isNotNull();
        assertThat(transactional.readOnly()).isFalse();
    }

    @Test
    void refusesToRejectActiveUserWithoutDeletingIdentifiers() {
        Usuario usuario = usuario(
                "ativa@fen.br",
                Role.FARMACEUTICO,
                SituacaoUsuario.ATIVO,
                LocalDateTime.of(2026, 8, 21, 17, 0)
        );
        when(usuarioRepository.findById(usuario.getId())).thenReturn(Optional.of(usuario));

        assertThatThrownBy(() -> usuarioService.rejeitar(usuario.getId()))
                .isInstanceOf(BusinessRuleException.class);
        verify(funcionarioRepository, never()).delete(any());
        verify(usuarioRepository, never()).delete(any());
    }

    private UsuarioRegisterRequest pharmacistRequest() {
        return pharmacistRequest(Role.FARMACEUTICO);
    }

    private UsuarioRegisterRequest pharmacistRequest(Role role) {
        return pharmacistRequest(role, "PR-12345");
    }

    private UsuarioRegisterRequest pharmacistRequest(Role role, String crf) {
        return new UsuarioRegisterRequest(
                "Nova Farmacêutica",
                "123.456.789-01",
                LocalDate.of(1990, 1, 1),
                "  NOVA@FEN.BR  ",
                "segredo123",
                role,
                crf,
                true,
                null,
                null,
                null,
                null
        );
    }

    private UsuarioRegisterRequest internRequest(
            UUID supervisorId,
            LocalDate inicioVigencia,
            LocalDate fimVigencia
    ) {
        return new UsuarioRegisterRequest(
                "Nova Estagiária",
                "987.654.321-00",
                LocalDate.of(2002, 2, 2),
                "estagiaria@fen.br",
                "segredo123",
                Role.ESTAGIARIO,
                null,
                null,
                TipoEstagio.OBRIGATORIO,
                supervisorId,
                inicioVigencia,
                fimVigencia
        );
    }

    private Funcionario supervisor(Role role, SituacaoUsuario situacao) {
        Usuario usuario = new Usuario();
        usuario.setRole(role);
        usuario.setSituacao(situacao);

        Funcionario funcionario = new Funcionario();
        funcionario.setId(UUID.fromString("00000000-0000-0000-0000-000000000203"));
        funcionario.setUsuario(usuario);
        funcionario.setNome("Supervisora");
        return funcionario;
    }

    private Usuario usuario(
            String email,
            Role role,
            SituacaoUsuario situacao,
            LocalDateTime createdAt
    ) {
        Usuario usuario = new Usuario();
        usuario.setId(UUID.fromString("00000000-0000-0000-0000-000000000210"));
        usuario.setEmail(email);
        usuario.setPasswordHash("hash-secreto");
        usuario.setRole(role);
        usuario.setSituacao(situacao);
        usuario.setCreatedAt(createdAt);
        return usuario;
    }

    private Funcionario funcionario(Usuario usuario, String nome, String cpf) {
        Funcionario funcionario = new Funcionario();
        funcionario.setId(UUID.fromString("00000000-0000-0000-0000-000000000220"));
        funcionario.setUsuario(usuario);
        funcionario.setNome(nome);
        funcionario.setCpf(cpf);
        return funcionario;
    }
}

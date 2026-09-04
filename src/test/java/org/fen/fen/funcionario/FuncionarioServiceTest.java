package org.fen.fen.funcionario;

import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.UUID;
import org.fen.fen.domain.Funcionario;
import org.fen.fen.domain.Role;
import org.fen.fen.domain.SituacaoUsuario;
import org.fen.fen.domain.Usuario;
import org.fen.fen.error.BusinessRuleException;
import org.fen.fen.mapper.FuncionarioMapper;
import org.fen.fen.repository.FuncionarioRepository;
import org.fen.fen.service.FuncionarioService;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.PageImpl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

class FuncionarioServiceTest {
    private final FuncionarioRepository repository = mock(FuncionarioRepository.class);
    private final FuncionarioService service = new FuncionarioService(repository, new FuncionarioMapper());

    @Test void listsMappedEmployees() {
        Funcionario employee = employee(Role.FARMACEUTICO, "PR-1");
        when(repository.buscar("ana", PageRequest.of(0, 10))).thenReturn(new PageImpl<>(List.of(employee)));
        assertThat(service.listar(" ana ", PageRequest.of(0, 10)).getContent()).singleElement()
                .satisfies(response -> {
                    assertThat(response.email()).isEqualTo("ana@fen.br");
                    assertThat(response.usuarioId()).isEqualTo(employee.getUsuario().getId());
                });
    }

    @Test void changesTechnicalResponsibilityForPharmacist() {
        Funcionario employee = employee(Role.FARMACEUTICO, "PR-1");
        when(repository.findDetailById(employee.getId())).thenReturn(Optional.of(employee));
        when(repository.save(employee)).thenReturn(employee);
        service.alterarResponsavelTecnico(employee.getId(), true);
        assertThat(employee.getResponsavelTecnico()).isTrue();
    }

    @Test void rejectsInternTechnicalResponsibility() {
        Funcionario employee = employee(Role.ESTAGIARIO, null);
        when(repository.findDetailById(employee.getId())).thenReturn(Optional.of(employee));
        assertThatThrownBy(() -> service.alterarResponsavelTecnico(employee.getId(), true)).isInstanceOf(BusinessRuleException.class);
        verify(repository, never()).save(any());
    }

    @Test void rejectsTechnicalResponsibilityForPendingPharmacist() {
        Funcionario employee = employee(Role.FARMACEUTICO, "PR-1");
        employee.getUsuario().setSituacao(SituacaoUsuario.PENDENTE);
        when(repository.findDetailById(employee.getId())).thenReturn(Optional.of(employee));

        assertThatThrownBy(() -> service.alterarResponsavelTecnico(employee.getId(), true))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessage("Funcionário ainda não foi efetivado");
        verify(repository, never()).save(any());
    }

    @Test void reportsMissingEmployee() {
        UUID id = UUID.randomUUID(); when(repository.findDetailById(id)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.buscar(id)).isInstanceOf(NoSuchElementException.class);
    }

    private Funcionario employee(Role role, String crf) {
        Usuario user = new Usuario(); user.setId(UUID.randomUUID()); user.setEmail("ana@fen.br"); user.setRole(role); user.setSituacao(SituacaoUsuario.ATIVO);
        Funcionario employee = new Funcionario(); employee.setId(UUID.randomUUID()); employee.setUsuario(user); employee.setNome("Ana"); employee.setCpf("12345678901"); employee.setCrf(crf); employee.setResponsavelTecnico(false); return employee;
    }
}

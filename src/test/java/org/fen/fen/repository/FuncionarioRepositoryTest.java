package org.fen.fen.repository;

import org.fen.fen.domain.Funcionario;
import org.fen.fen.domain.Role;
import org.fen.fen.domain.SituacaoUsuario;
import org.fen.fen.domain.Usuario;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.context.jdbc.Sql;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@Sql("/datasets/usuario-repository.sql")
class FuncionarioRepositoryTest extends BaseRepositoryTest {

    private static final UUID FARMACEUTICA_ATIVA_USUARIO_ID =
            UUID.fromString("00000000-0000-0000-0000-000000000101");

    @Autowired
    private FuncionarioRepository funcionarioRepository;

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Test
    void savesFuncionarioWithGeneratedUuidAndRequiredUsuario() {
        Usuario usuario = usuarioRepository.saveAndFlush(usuario("nova.funcionaria@fen.br"));
        Funcionario funcionario = funcionario("Nova Funcionária", "123.456.789-01", usuario);

        Funcionario saved = funcionarioRepository.saveAndFlush(funcionario);

        assertThat(saved.getId()).isNotNull();
        assertThat(saved.getUsuario().getId()).isEqualTo(usuario.getId());
    }

    @Test
    void findsFuncionarioByUsuarioId() {
        assertThat(funcionarioRepository.findByUsuarioId(FARMACEUTICA_ATIVA_USUARIO_ID))
                .get()
                .extracting(Funcionario::getNome)
                .isEqualTo("Farmacêutica Ativa");
    }

    @Test
    void rejectsASecondFuncionarioForTheSameUsuario() {
        Usuario usuario = usuarioRepository.findById(FARMACEUTICA_ATIVA_USUARIO_ID).orElseThrow();
        Funcionario duplicateAssociation = funcionario("Outra Funcionária", "12345678901", usuario);

        assertThatThrownBy(() -> funcionarioRepository.saveAndFlush(duplicateAssociation))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void rejectsDuplicateNormalizedCpf() {
        Usuario usuario = usuarioRepository.saveAndFlush(usuario("outra@fen.br"));
        Funcionario duplicateCpf = funcionario("Outra Funcionária", "111.111.111-11", usuario);

        assertThatThrownBy(() -> funcionarioRepository.saveAndFlush(duplicateCpf))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void findsOnlyActiveNonInternSupervisorsOrderedByName() {
        assertThat(funcionarioRepository.findSupervisoresAtivos())
                .extracting(
                        Funcionario::getNome,
                        funcionario -> funcionario.getUsuario().getRole()
                )
                .containsExactly(
                        org.assertj.core.groups.Tuple.tuple("Administrador Elegível", Role.ADMIN),
                        org.assertj.core.groups.Tuple.tuple("Farmacêutica Ativa", Role.FARMACEUTICO),
                        org.assertj.core.groups.Tuple.tuple("Zélia Farmacêutica", Role.FARMACEUTICO)
                );
    }

    private Usuario usuario(String email) {
        Usuario usuario = new Usuario();
        usuario.setEmail(email);
        usuario.setPasswordHash("hash");
        usuario.setRole(Role.FARMACEUTICO);
        usuario.setSituacao(SituacaoUsuario.PENDENTE);
        return usuario;
    }

    private Funcionario funcionario(String nome, String cpf, Usuario usuario) {
        Funcionario funcionario = new Funcionario();
        funcionario.setNome(nome);
        funcionario.setCpf(cpf);
        funcionario.setUsuario(usuario);
        return funcionario;
    }
}

package org.fen.fen.repository;

import org.fen.fen.domain.Usuario;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.test.context.jdbc.Sql;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@Sql(value = {
        "classpath:/usuario.sql"
})
public class UsuarioRepositoryTest extends BaseRepositoryTest {

    @Autowired
    private UsuarioRepository repository;

    private static final String EMAIL = "carlos.santos@email.com";

    @Test
    void findAll() {
        List<Usuario> usuarios = this.repository.findAll();
        assertThat(usuarios).isNotEmpty().hasSize(5);
    }

    @Test
    void findByEmail() {
        Optional<Usuario> optUsuario = this.repository.findByEmail(EMAIL);
        assertThat(optUsuario).isNotEmpty();
    }

    @Test
    void save() {
        Usuario usuario = new Usuario();
        usuario.setNome("Teste da silva");
        usuario.setDataNascimento(LocalDate.now());
        usuario.setEmail("teste.silva@gmail.com");
        usuario.setHashSenha("klsjdlasuiouro234398wd9sad8sjd#/3@@@");
        usuario.setCpf("33817238411");

        Usuario saved = this.repository.saveAndFlush(usuario);

        assertThat(saved.getId()).isNotNull();
        assertThat(saved).extracting(
                Usuario::getNome,
                Usuario::getDataNascimento,
                Usuario::getEmail,
                Usuario::getHashSenha,
                Usuario::getCpf
        ).containsAll(
                List.of(
                        usuario.getNome(),
                        usuario.getDataNascimento(),
                        usuario.getEmail(),
                        usuario.getHashSenha(),
                        usuario.getCpf()
                )
        );
    }

    @Test
    void update() {
        Usuario usuario = this.repository.findByEmail(EMAIL).orElseThrow(() -> new UsernameNotFoundException("Email não encontrado."));

        final String nomeAntigo = usuario.getNome();

        usuario.setNome("Nome alterado");

        Usuario updated = this.repository.saveAndFlush(usuario);

        assertThat(updated.getNome()).isNotEqualTo(nomeAntigo);
    }

    @Test
    void deleteById() {
        Usuario usuario = this.repository.findByEmail(EMAIL).orElseThrow(() -> new UsernameNotFoundException("Email não encontrado."));

        this.repository.deleteById(usuario.getId());

        Optional<Usuario> shouldBeEmpty = this.repository.findByEmail(EMAIL);

        assertThat(shouldBeEmpty).isEmpty();
    }

}

package org.fen.fen.repository;
import org.fen.fen.domain.Paciente; import org.junit.jupiter.api.Test; import org.springframework.beans.factory.annotation.Autowired; import java.time.LocalDate; import static org.assertj.core.api.Assertions.*;
class PacienteRepositoryTest extends BaseRepositoryTest {
 @Autowired PacienteRepository repository;
 @Test void buscaCpfNormalizadoEPreservaUnicidade(){Paciente p=new Paciente();p.setNome("Maria");p.setCpf("12345678901");p.setDataNascimento(LocalDate.of(1990,1,1));p.setCelular("44999999999");repository.saveAndFlush(p);assertThat(repository.findByCpf("12345678901")).isPresent();assertThat(repository.buscar("Maria","",org.springframework.data.domain.PageRequest.of(0,10)).getTotalElements()).isEqualTo(1);}
}

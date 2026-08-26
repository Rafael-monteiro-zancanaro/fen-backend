package org.fen.fen.service;
import org.fen.fen.domain.*; import org.fen.fen.error.ConflictException; import org.fen.fen.paciente.dto.PacienteRequest; import org.fen.fen.repository.*; import org.junit.jupiter.api.*; import java.time.LocalDate; import java.util.*; import static org.assertj.core.api.Assertions.*; import static org.mockito.Mockito.*;
class PacienteServiceTest {
 private final PacienteRepository patients=mock(PacienteRepository.class); private final ComorbidadeRepository comorbidities=mock(ComorbidadeRepository.class); private PacienteService service;
 @BeforeEach void setup(){service=new PacienteService(patients,comorbidities);}
 @Test void recusaCpfDuplicado(){when(patients.existsByCpf("12345678901")).thenReturn(true);assertThatThrownBy(()->service.criar(request())).isInstanceOf(ConflictException.class);}
 @Test void associaComorbidadeExistente(){Comorbidade c=new Comorbidade("Diabetes");c.setId(UUID.randomUUID());when(comorbidities.findAllById(any())).thenReturn(List.of(c));when(patients.saveAndFlush(any())).thenAnswer(i->{Paciente p=i.getArgument(0);p.setId(UUID.randomUUID());return p;});var response=service.criar(request(c.getId()));assertThat(response.cpf()).isEqualTo("12345678901");assertThat(response.comorbidityIds()).containsExactly(c.getId());}
 private PacienteRequest request(UUID... ids){return new PacienteRequest("Maria","123.456.789-01",LocalDate.of(1990,1,1),"(44) 99999-9999","feminino","","","","","PR","","",List.of(ids));}
}

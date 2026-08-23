package org.fen.fen.service;

import org.fen.fen.comorbidade.dto.ComorbidadeRequest;
import org.fen.fen.domain.Comorbidade;
import org.fen.fen.domain.Medicamento;
import org.fen.fen.error.BusinessRuleException;
import org.fen.fen.mapper.ComorbidadeMapper;
import org.fen.fen.mapper.MedicamentoMapper;
import org.fen.fen.repository.ComorbidadeRepository;
import org.fen.fen.repository.MedicamentoRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ComorbidadeSyncServiceTest {
    private final ComorbidadeRepository comorbidades = mock(ComorbidadeRepository.class);
    private final MedicamentoRepository medicamentos = mock(MedicamentoRepository.class);
    private ComorbidadeService service;

    @BeforeEach
    void setUp() {
        service = new ComorbidadeService(comorbidades, medicamentos,
                new ComorbidadeMapper(new MedicamentoMapper()));
    }

    @Test
    void mantemInteracaoExistenteERemoveSomenteAExcluidaDaSelecao() {
        UUID comorbidadeId = UUID.randomUUID();
        Medicamento a = medicamento(UUID.randomUUID(), "Dipirona");
        Medicamento b = medicamento(UUID.randomUUID(), "Ibuprofeno");
        Comorbidade comorbidade = new Comorbidade("Hipertensão");
        var mantida = comorbidade.adicionarInteracao(a);
        comorbidade.adicionarInteracao(b);
        when(comorbidades.buscarDetalhe(comorbidadeId)).thenReturn(Optional.of(comorbidade));
        when(comorbidades.saveAndFlush(comorbidade)).thenReturn(comorbidade);

        service.atualizar(comorbidadeId, new ComorbidadeRequest("Hipertensão", List.of(a.getId())));

        assertThat(comorbidade.getInteracoes()).containsExactly(mantida);
    }

    @Test
    void rejeitaIdDeMedicamentoDuplicadoNoRequest() {
        UUID id = UUID.randomUUID();
        assertThatThrownBy(() -> service.criar(new ComorbidadeRequest("Hipertensão", List.of(id, id))))
                .isInstanceOf(BusinessRuleException.class);
    }

    private Medicamento medicamento(UUID id, String nome) {
        Medicamento medicamento = new Medicamento(nome, "mg", "Oral");
        medicamento.setId(id);
        return medicamento;
    }
}

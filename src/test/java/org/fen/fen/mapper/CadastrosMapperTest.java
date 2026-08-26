package org.fen.fen.mapper;

import org.fen.fen.domain.Comorbidade;
import org.fen.fen.domain.Medicamento;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class CadastrosMapperTest {

    @Test
    void mapeiaMedicamentoParaContratoAngular() {
        Medicamento medicamento = new Medicamento("Dipirona", "mg", "Oral");
        medicamento.setId(UUID.fromString("550e8400-e29b-41d4-a716-446655440000"));

        var response = new MedicamentoMapper().toResponse(medicamento);

        assertThat(response.name()).isEqualTo("Dipirona");
        assertThat(response.measurementUnit()).isEqualTo("mg");
        assertThat(response.administrationRoute()).isEqualTo("Oral");
    }

    @Test
    void detalheDaComorbidadeIncluiIdsENomesSemConsultaAdicional() {
        Medicamento medicamento = new Medicamento("Dipirona", "mg", "Oral");
        medicamento.setId(UUID.fromString("550e8400-e29b-41d4-a716-446655440000"));
        Comorbidade comorbidade = new Comorbidade("Hipertensão");
        comorbidade.adicionarInteracao(medicamento);

        var response = new ComorbidadeMapper(new MedicamentoMapper()).toResponse(comorbidade);

        assertThat(response.medicationInteractionIds()).containsExactly(medicamento.getId());
        assertThat(response.interactionMedications()).extracting("name").containsExactly("Dipirona");
    }
}

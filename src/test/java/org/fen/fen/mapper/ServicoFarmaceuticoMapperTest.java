package org.fen.fen.mapper;

import org.fen.fen.domain.Medicamento;
import org.fen.fen.domain.MedicamentoAtendimento;
import org.fen.fen.domain.ServicoFarmaceutico;
import org.fen.fen.domain.TipoServicoMedicamento;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

class ServicoFarmaceuticoMapperTest {

    @Test
    void detalhaMedicamentoComDadosEspecificosDaAplicacaoEViaDoCadastro() {
        Medicamento medicamento = new Medicamento("Dipirona", "500 mg", "Intramuscular");
        MedicamentoAtendimento item = new MedicamentoAtendimento();
        item.setMedicamento(medicamento);
        item.setTipoServico(TipoServicoMedicamento.APLICACAO_INJETAVEIS);
        item.setLote("A-10");
        item.setValidade(LocalDate.of(2027, 1, 1));
        item.setPosologia("1 ampola");
        item.setNomePrescritor("Dra. Ana");
        item.setRegistroPrescritor("CRM 123");

        ServicoFarmaceutico atendimento = new ServicoFarmaceutico();
        atendimento.adicionarMedicamento(item);

        var response = ServicoFarmaceuticoMapper.medication(item);

        assertThat(response.medicationConcentration()).isEqualTo("Dipirona — 500 mg");
        assertThat(response.administrationRoute()).isEqualTo("Intramuscular");
        assertThat(response.prescriberName()).isEqualTo("Dra. Ana");
        assertThat(response.prescriberRegistration()).isEqualTo("CRM 123");
    }

    @Test
    void preservaMedicamentoHistoricoDeServicosSemDuplicaLoNoAcompanhamentoFarmacoterapeutico() {
        Medicamento medicamento = new Medicamento("Paracetamol", "750 mg", "Oral");
        MedicamentoAtendimento item = new MedicamentoAtendimento();
        item.setMedicamento(medicamento);
        item.setTipoServico(TipoServicoMedicamento.SERVICOS_FARMACEUTICOS);
        item.setLote("LEG-01");
        item.setValidade(LocalDate.of(2027, 1, 1));
        item.setPosologia("1 comprimido");

        ServicoFarmaceutico atendimento = new ServicoFarmaceutico();
        atendimento.setAcompanhamentoFarmacoterapeutico(true);
        atendimento.adicionarMedicamento(item);

        var acompanhamento = ServicoFarmaceuticoMapper.pharmacotherapeuticFollowUp(atendimento);

        assertThat(ServicoFarmaceuticoMapper.medications(
                atendimento, TipoServicoMedicamento.SERVICOS_FARMACEUTICOS
        )).extracting(response -> response.batch()).containsExactly("LEG-01");
        assertThat(acompanhamento).isNotNull();
        assertThat(acompanhamento.medications()).isEmpty();
    }
}

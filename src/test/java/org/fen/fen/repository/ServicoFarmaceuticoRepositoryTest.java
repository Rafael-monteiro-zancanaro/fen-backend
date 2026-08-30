package org.fen.fen.repository;

import org.fen.fen.domain.Medicamento;
import org.fen.fen.domain.MedicamentoAtendimento;
import org.fen.fen.domain.Paciente;
import org.fen.fen.domain.ServicoFarmaceutico;
import org.fen.fen.domain.StatusServicoFarmaceutico;
import org.fen.fen.domain.TipoServicoMedicamento;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

class ServicoFarmaceuticoRepositoryTest extends BaseRepositoryTest {

    @Autowired
    private ServicoFarmaceuticoRepository repository;

    @Autowired
    private PacienteRepository pacienteRepository;

    @Autowired
    private MedicamentoRepository medicamentoRepository;

    @Test
    void persisteAtendimentoECarregaMedicamentoAssociadoNoDetalhe() {
        Paciente paciente = pacienteRepository.saveAndFlush(paciente());
        Medicamento medicamento = medicamentoRepository.saveAndFlush(
                new Medicamento("Dipirona", "500 mg", "Intramuscular")
        );
        ServicoFarmaceutico atendimento = new ServicoFarmaceutico();
        atendimento.setCodigo(1001L);
        atendimento.setPaciente(paciente);
        atendimento.setDataAtendimento(LocalDate.of(2026, 8, 28));
        atendimento.setStatusPersistido(StatusServicoFarmaceutico.CONCLUIDO);
        atendimento.setNumeroRetorno(0);

        MedicamentoAtendimento item = new MedicamentoAtendimento();
        item.setMedicamento(medicamento);
        item.setTipoServico(TipoServicoMedicamento.APLICACAO_INJETAVEIS);
        item.setLote("LOTE-01");
        item.setValidade(LocalDate.of(2027, 1, 1));
        item.setPosologia("1 ampola");
        item.setNomePrescritor("Dra. Ana");
        item.setRegistroPrescritor("CRM 123");
        atendimento.adicionarMedicamento(item);

        MedicamentoAtendimento inaloterapia = new MedicamentoAtendimento();
        inaloterapia.setMedicamento(medicamento);
        inaloterapia.setTipoServico(TipoServicoMedicamento.INALOTERAPIA);
        inaloterapia.setLote("LOTE-02");
        inaloterapia.setValidade(LocalDate.of(2027, 2, 1));
        inaloterapia.setPosologia("2 jatos");
        inaloterapia.setNomePrescritor("Dr. Bruno");
        inaloterapia.setRegistroPrescritor("CRM 456");
        atendimento.adicionarMedicamento(inaloterapia);

        ServicoFarmaceutico salvo = repository.saveAndFlush(atendimento);
        ServicoFarmaceutico detalhe = repository.buscarDetalhe(salvo.getId()).orElseThrow();

        assertThat(detalhe.getMedicamentosAtendimento()).hasSize(2);
        assertThat(detalhe.getMedicamentosAtendimento()).anySatisfy(registrado -> {
            assertThat(registrado.getTipoServico()).isEqualTo(TipoServicoMedicamento.APLICACAO_INJETAVEIS);
            assertThat(registrado.getMedicamento().getId()).isEqualTo(medicamento.getId());
            assertThat(registrado.getLote()).isEqualTo("LOTE-01");
            assertThat(registrado.getNomePrescritor()).isEqualTo("Dra. Ana");
            assertThat(registrado.getRegistroPrescritor()).isEqualTo("CRM 123");
        });
        assertThat(detalhe.getMedicamentosAtendimento()).anySatisfy(registrado -> {
            assertThat(registrado.getTipoServico()).isEqualTo(TipoServicoMedicamento.INALOTERAPIA);
            assertThat(registrado.getLote()).isEqualTo("LOTE-02");
            assertThat(registrado.getNomePrescritor()).isEqualTo("Dr. Bruno");
            assertThat(registrado.getRegistroPrescritor()).isEqualTo("CRM 456");
        });
    }

    private Paciente paciente() {
        Paciente paciente = new Paciente();
        paciente.setNome("Maria da Silva");
        paciente.setCpf("12345678901");
        paciente.setDataNascimento(LocalDate.of(1990, 1, 1));
        paciente.setCelular("44999999999");
        return paciente;
    }
}

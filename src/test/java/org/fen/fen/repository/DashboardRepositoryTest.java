package org.fen.fen.repository;

import org.fen.fen.domain.Acompanhamento;
import org.fen.fen.domain.CuidadosFarmaceuticos;
import org.fen.fen.domain.DadosServicosFarmaceuticos;
import org.fen.fen.domain.Medicamento;
import org.fen.fen.domain.MedicamentoAtendimento;
import org.fen.fen.domain.Paciente;
import org.fen.fen.domain.ServicoFarmaceutico;
import org.fen.fen.domain.StatusServicoFarmaceutico;
import org.fen.fen.domain.TipoServicoMedicamento;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

class DashboardRepositoryTest extends BaseRepositoryTest {

    private static final LocalDate TODAY = LocalDate.of(2026, 9, 2);

    @Autowired
    private ServicoFarmaceuticoRepository repository;

    @Autowired
    private AcompanhamentoRepository acompanhamentoRepository;

    @Autowired
    private PacienteRepository pacienteRepository;

    @Autowired
    private MedicamentoRepository medicamentoRepository;

    @Test
    void agregaSomenteAtendimentosAtuaisAtivosEContaCadaTipoUmaVezPorAtendimento() {
        Paciente patient = patient();
        Medicamento medication = medication();

        ServicoFarmaceutico concluded = save(patient, 1001L, StatusServicoFarmaceutico.CONCLUIDO);
        CuidadosFarmaceuticos care = new CuidadosFarmaceuticos();
        care.setGlicemiaCapilar(BigDecimal.valueOf(95));
        concluded.setCuidadosFarmaceuticos(care);
        adicionarMedicamento(concluded, medication, TipoServicoMedicamento.APLICACAO_INJETAVEIS, "INJ-01");
        adicionarMedicamento(concluded, medication, TipoServicoMedicamento.APLICACAO_INJETAVEIS, "INJ-02");
        repository.saveAndFlush(concluded);

        ServicoFarmaceutico tomorrow = acompanhar(patient, 1002L, TODAY.plusDays(1));
        adicionarMedicamento(tomorrow, medication, TipoServicoMedicamento.INALOTERAPIA, "INA-01");
        repository.saveAndFlush(tomorrow);

        ServicoFarmaceutico today = acompanhar(patient, 1003L, TODAY);
        DadosServicosFarmaceuticos complementary = new DadosServicosFarmaceuticos();
        complementary.setAssistenciaDomiciliar(false);
        complementary.setAcompanhamentoFarmacoterapeutico(true);
        complementary.setIndicacaoTranstornosMenores(false);
        today.setDadosServicosFarmaceuticos(complementary);
        repository.saveAndFlush(today);

        acompanhar(patient, 1004L, TODAY.minusDays(1));

        ServicoFarmaceutico stale = save(patient, 1005L, StatusServicoFarmaceutico.AGUARDANDO_RETORNO);
        adicionarMedicamento(stale, medication, TipoServicoMedicamento.APLICACAO_INJETAVEIS, "INJ-03");
        repository.saveAndFlush(stale);
        ServicoFarmaceutico current = save(patient, 1006L, StatusServicoFarmaceutico.CONCLUIDO);
        current.setNumeroRetorno(1);
        Acompanhamento chain = new Acompanhamento();
        chain.setAtendimentoInicial(stale);
        chain.setAtendimentoAtual(current);
        chain.setIntervaloRetornoDias(7);
        chain.setQuantidadeRetornos(2);
        chain.setProximoRetorno(TODAY);
        acompanhamentoRepository.saveAndFlush(chain);
        stale.setAcompanhamento(chain);
        current.setAcompanhamento(chain);
        repository.saveAndFlush(stale);
        repository.saveAndFlush(current);

        assertThat(repository.contarAguardandoRetornoAtivoEmOrAfter(TODAY)).isEqualTo(2L);
        assertThat(repository.contarReconsultasAtivasEm(TODAY)).isEqualTo(1L);
        assertThat(repository.contarExpiradosAtivosAntesDe(TODAY)).isEqualTo(1L);
        assertThat(repository.contarCuidadosFarmaceuticos()).isEqualTo(1L);
        assertThat(repository.contarAplicacoesInjetaveis()).isEqualTo(2L);
        assertThat(repository.contarInaloterapias()).isEqualTo(1L);
        assertThat(repository.contarServicosFarmaceuticos()).isEqualTo(1L);
        assertThat(repository.countByStatusPersistido(StatusServicoFarmaceutico.CONCLUIDO)).isEqualTo(2L);

        assertThat(repository.listar("", "", "AGUARDANDO_RETORNO", false, TODAY, PageRequest.of(0, 10))
                .getTotalElements()).isEqualTo(2L);
        assertThat(repository.listar("", "", "EXPIRADO", false, TODAY, PageRequest.of(0, 10))
                .getTotalElements()).isEqualTo(1L);
        assertThat(repository.listar("", "", "", true, TODAY, PageRequest.of(0, 10))
                .getTotalElements()).isEqualTo(1L);
    }

    private Paciente patient() {
        Paciente patient = new Paciente();
        patient.setNome("Maria da Silva");
        patient.setCpf("12345678901");
        patient.setDataNascimento(LocalDate.of(1990, 1, 1));
        patient.setCelular("44999999999");
        return pacienteRepository.saveAndFlush(patient);
    }

    private Medicamento medication() {
        return medicamentoRepository.saveAndFlush(new Medicamento("Dipirona", "500 mg", "Intramuscular"));
    }

    private ServicoFarmaceutico acompanhar(Paciente patient, long code, LocalDate dueDate) {
        ServicoFarmaceutico attendance = save(patient, code, StatusServicoFarmaceutico.AGUARDANDO_RETORNO);
        Acompanhamento followUp = new Acompanhamento();
        followUp.setAtendimentoInicial(attendance);
        followUp.setAtendimentoAtual(attendance);
        followUp.setIntervaloRetornoDias(7);
        followUp.setQuantidadeRetornos(2);
        followUp.setProximoRetorno(dueDate);
        acompanhamentoRepository.saveAndFlush(followUp);
        attendance.setAcompanhamento(followUp);
        return repository.saveAndFlush(attendance);
    }

    private ServicoFarmaceutico save(Paciente patient, long code, StatusServicoFarmaceutico status) {
        ServicoFarmaceutico attendance = new ServicoFarmaceutico();
        attendance.setCodigo(code);
        attendance.setPaciente(patient);
        attendance.setDataAtendimento(TODAY);
        attendance.setStatusPersistido(status);
        attendance.setNumeroRetorno(0);
        return repository.saveAndFlush(attendance);
    }

    private void adicionarMedicamento(
            ServicoFarmaceutico attendance,
            Medicamento medication,
            TipoServicoMedicamento serviceType,
            String batch
    ) {
        MedicamentoAtendimento item = new MedicamentoAtendimento();
        item.setMedicamento(medication);
        item.setTipoServico(serviceType);
        item.setLote(batch);
        item.setValidade(LocalDate.of(2027, 1, 1));
        item.setPosologia("1 dose");
        attendance.adicionarMedicamento(item);
    }
}

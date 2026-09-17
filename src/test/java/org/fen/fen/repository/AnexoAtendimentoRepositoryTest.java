package org.fen.fen.repository;

import org.fen.fen.domain.AnexoAtendimento;
import org.fen.fen.domain.Paciente;
import org.fen.fen.domain.ServicoFarmaceutico;
import org.fen.fen.domain.StatusServicoFarmaceutico;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.LocalDate;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class AnexoAtendimentoRepositoryTest extends BaseRepositoryTest {

    @Autowired
    private AnexoAtendimentoRepository anexoRepository;

    @Autowired
    private ServicoFarmaceuticoRepository servicoRepository;

    @Autowired
    private PacienteRepository pacienteRepository;

    @Test
    void contaAnexosDosAtendimentosSolicitadosEmLote() {
        ServicoFarmaceutico primeiro = atendimento("12345678901", 1001L);
        ServicoFarmaceutico segundo = atendimento("10987654321", 1002L);
        anexoRepository.saveAndFlush(anexo(primeiro, "receita.pdf"));
        anexoRepository.saveAndFlush(anexo(primeiro, "resultado.pdf"));
        anexoRepository.saveAndFlush(anexo(segundo, "foto.png"));

        Map<UUID, Long> contagens = anexoRepository.countByServicoFarmaceuticoIdIn(
                        java.util.List.of(primeiro.getId(), segundo.getId()))
                .stream()
                .collect(java.util.stream.Collectors.toMap(
                        AnexoAtendimentoRepository.AttachmentCount::getServicoFarmaceuticoId,
                        AnexoAtendimentoRepository.AttachmentCount::getAttachmentCount
                ));

        assertThat(contagens).containsEntry(primeiro.getId(), 2L).containsEntry(segundo.getId(), 1L);
    }

    private ServicoFarmaceutico atendimento(String cpf, long codigo) {
        Paciente paciente = new Paciente();
        paciente.setNome("Paciente de teste");
        paciente.setCpf(cpf);
        paciente.setDataNascimento(LocalDate.of(1990, 1, 1));
        paciente.setCelular("44999999999");
        paciente = pacienteRepository.saveAndFlush(paciente);

        ServicoFarmaceutico atendimento = new ServicoFarmaceutico();
        atendimento.setCodigo(codigo);
        atendimento.setPaciente(paciente);
        atendimento.setDataAtendimento(LocalDate.of(2026, 9, 17));
        atendimento.setStatusPersistido(StatusServicoFarmaceutico.CONCLUIDO);
        atendimento.setNumeroRetorno(0);
        return servicoRepository.saveAndFlush(atendimento);
    }

    private AnexoAtendimento anexo(ServicoFarmaceutico atendimento, String nomeOriginal) {
        AnexoAtendimento anexo = new AnexoAtendimento();
        anexo.setServicoFarmaceutico(atendimento);
        anexo.setNomeOriginal(nomeOriginal);
        anexo.setChaveArmazenamento(UUID.randomUUID().toString());
        anexo.setContentType("application/pdf");
        anexo.setTamanho(32L);
        return anexo;
    }
}

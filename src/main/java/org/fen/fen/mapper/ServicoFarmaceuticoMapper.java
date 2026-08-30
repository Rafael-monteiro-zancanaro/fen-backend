package org.fen.fen.mapper;

import org.fen.fen.domain.Acompanhamento;
import org.fen.fen.domain.MedicamentoAtendimento;
import org.fen.fen.domain.ServicoFarmaceutico;
import org.fen.fen.domain.TipoServicoMedicamento;
import org.fen.fen.servicofarmaceutico.dto.ServicoFarmaceuticoResponse;

import java.math.BigDecimal;
import java.util.List;

public final class ServicoFarmaceuticoMapper {

    private ServicoFarmaceuticoMapper() {
    }

    public static ServicoFarmaceuticoResponse.Care care(ServicoFarmaceutico atendimento) {
        if (atendimento.getCuidadosFarmaceuticos() == null) {
            return null;
        }

        var cuidados = atendimento.getCuidadosFarmaceuticos();
        return new ServicoFarmaceuticoResponse.Care(
                number(cuidados.getGlicemiaCapilar()),
                number(cuidados.getPressaoSistolica()),
                number(cuidados.getPressaoDiastolica()),
                number(cuidados.getTemperaturaCorporal())
        );
    }

    public static ServicoFarmaceuticoResponse.Injectable injectable(ServicoFarmaceutico atendimento) {
        List<ServicoFarmaceuticoResponse.Medication> medicamentos = medications(
                atendimento,
                TipoServicoMedicamento.APLICACAO_INJETAVEIS
        );
        if (medicamentos.isEmpty()) {
            return null;
        }

        return new ServicoFarmaceuticoResponse.Injectable(medicamentos);
    }

    public static ServicoFarmaceuticoResponse.Inhalotherapy inhalotherapy(ServicoFarmaceutico atendimento) {
        List<ServicoFarmaceuticoResponse.Medication> medicamentos = medications(
                atendimento,
                TipoServicoMedicamento.INALOTERAPIA
        );
        if (medicamentos.isEmpty()) {
            return null;
        }

        return new ServicoFarmaceuticoResponse.Inhalotherapy(medicamentos);
    }

    public static ServicoFarmaceuticoResponse.ComplementaryServices complementaryServices(
            ServicoFarmaceutico atendimento
    ) {
        if (atendimento.getDadosServicosFarmaceuticos() == null) {
            return null;
        }

        var servicos = atendimento.getDadosServicosFarmaceuticos();
        return new ServicoFarmaceuticoResponse.ComplementaryServices(
                servicos.getAssistenciaDomiciliar(),
                servicos.getAcompanhamentoFarmacoterapeutico(),
                servicos.getIndicacaoTranstornosMenores(),
                servicos.getSinaisESintomas(),
                medications(atendimento, TipoServicoMedicamento.SERVICOS_FARMACEUTICOS)
        );
    }

    public static List<ServicoFarmaceuticoResponse.Medication> medications(
            ServicoFarmaceutico atendimento,
            TipoServicoMedicamento tipo
    ) {
        return atendimento.getMedicamentosAtendimento().stream()
                .filter(item -> item.getTipoServico() == tipo)
                .map(ServicoFarmaceuticoMapper::medication)
                .toList();
    }

    public static ServicoFarmaceuticoResponse.Medication medication(MedicamentoAtendimento item) {
        var medicamento = item.getMedicamento();
        return new ServicoFarmaceuticoResponse.Medication(
                item.getId(),
                medicamento.getId(),
                medicamento.getNome() + " — " + medicamento.getUnidadeMedida(),
                item.getLote(),
                item.getValidade(),
                item.getPosologia(),
                medicamento.getViaAdministracao(),
                item.getNomePrescritor(),
                item.getRegistroPrescritor()
        );
    }

    public static ServicoFarmaceuticoResponse.FollowUp followUp(Acompanhamento acompanhamento) {
        if (acompanhamento == null) {
            return null;
        }

        return new ServicoFarmaceuticoResponse.FollowUp(
                acompanhamento.getIntervaloRetornoDias(),
                acompanhamento.getQuantidadeRetornos()
        );
    }

    private static String number(BigDecimal value) {
        return value == null ? "" : value.stripTrailingZeros().toPlainString();
    }
}

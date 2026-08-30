package org.fen.fen.servicofarmaceutico;

import org.fen.fen.domain.Acompanhamento;
import org.fen.fen.domain.ServicoFarmaceutico;
import org.fen.fen.domain.StatusServicoFarmaceutico;
import org.fen.fen.service.StatusEfetivoResolver;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;

import static org.assertj.core.api.Assertions.assertThat;

class StatusEfetivoResolverTest {

    private final StatusEfetivoResolver resolver = new StatusEfetivoResolver(
            Clock.fixed(Instant.parse("2026-08-21T12:00:00Z"), ZoneOffset.UTC)
    );

    @Test
    void retornaExpiradoQuandoAtendimentoAguardandoPossuiPrazoAnteriorAoRelogio() {
        ServicoFarmaceutico atendimento = new ServicoFarmaceutico();
        atendimento.setStatusPersistido(StatusServicoFarmaceutico.AGUARDANDO_RETORNO);
        Acompanhamento acompanhamento = new Acompanhamento();
        acompanhamento.setProximoRetorno(LocalDate.of(2026, 8, 20));
        atendimento.setAcompanhamento(acompanhamento);

        assertThat(resolver.resolver(atendimento)).isEqualTo(StatusServicoFarmaceutico.EXPIRADO);
    }

    @Test
    void mantemConcluidoQuandoPrazoDoAcompanhamentoEstaVencido() {
        ServicoFarmaceutico atendimento = new ServicoFarmaceutico();
        atendimento.setStatusPersistido(StatusServicoFarmaceutico.CONCLUIDO);
        Acompanhamento acompanhamento = new Acompanhamento();
        acompanhamento.setProximoRetorno(LocalDate.of(2026, 8, 20));
        atendimento.setAcompanhamento(acompanhamento);

        assertThat(resolver.resolver(atendimento)).isEqualTo(StatusServicoFarmaceutico.CONCLUIDO);
    }
}

package org.fen.fen.servicofarmaceutico.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import org.fen.fen.paciente.dto.PacienteRequest;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public record ServicoFarmaceuticoRequest(
        UUID patientId, @Valid PacienteRequest patient, @Valid Care care,
        @Valid Injectable injectable, @Valid Inhalotherapy inhalotherapy,
        @Valid ComplementaryServices complementaryServices, @Valid FollowUp followUp) {
    public record Care(
            BigDecimal bloodGlucose,
            BigDecimal systolicPressure,
            BigDecimal diastolicPressure,
            BigDecimal bodyTemperature
    ) {
    }

    public record Injectable(@NotNull List<@Valid MedicamentoAtendimentoRequest> medications) {
    }

    public record Inhalotherapy(@NotNull List<@Valid MedicamentoAtendimentoRequest> medications) {
    }

    public record ComplementaryServices(
            Boolean homeCare,
            Boolean pharmacotherapeuticFollowUp,
            Boolean minorDisorderIndication,
            String signsAndSymptoms,
            @NotNull List<@Valid MedicamentoAtendimentoRequest> medications
    ) {
    }

    public record FollowUp(@Positive Integer returnIntervalDays, @Positive Integer returnCount) {
    }

    @AssertTrue(message = "Informe o paciente do atendimento")
    public boolean hasPatient() {
        return patientId != null || patient != null;
    }
}

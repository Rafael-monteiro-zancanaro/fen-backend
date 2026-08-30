package org.fen.fen.servicofarmaceutico.dto;

import org.fen.fen.domain.StatusServicoFarmaceutico;
import org.fen.fen.paciente.dto.PacienteResponse;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public record ServicoFarmaceuticoResponse(
        UUID id,
        Long codigo,
        PacienteResponse patient,
        List<String> selectedServices,
        LocalDate attendanceDate,
        LocalDateTime createdAt,
        StatusServicoFarmaceutico status,
        Care care,
        Injectable injectable,
        Inhalotherapy inhalotherapy,
        ComplementaryServices complementaryServices,
        FollowUp followUp,
        FollowUpLink followUpLink,
        FollowUpProgress followUpProgress,
        List<FollowUpHistoryEntry> followUpHistory,
        boolean editAllowed
) {
    public record Care(
            String bloodGlucose,
            String systolicPressure,
            String diastolicPressure,
            String bodyTemperature
    ) {
    }

    public record Medication(
            UUID id,
            UUID medicationId,
            String medicationConcentration,
            String batch,
            LocalDate expirationDate,
            String dosage,
            String administrationRoute,
            String prescriberName,
            String prescriberRegistration
    ) {
    }

    public record Injectable(List<Medication> medications) {
    }

    public record Inhalotherapy(List<Medication> medications) {
    }

    public record ComplementaryServices(
            Boolean homeCare,
            Boolean pharmacotherapeuticFollowUp,
            Boolean minorDisorderIndication,
            String signsAndSymptoms,
            List<Medication> medications
    ) {
    }

    public record FollowUp(Integer returnIntervalDays, Integer returnCount) {
    }

    public record FollowUpLink(
            UUID chainId,
            UUID originAttendanceId,
            UUID previousAttendanceId,
            Integer returnNumber
    ) {
    }

    public record FollowUpProgress(
            Integer returnCount,
            Integer completedReturns,
            Integer nextReturnNumber,
            boolean canContinue
    ) {
    }

    public record FollowUpHistoryEntry(
            String label,
            UUID attendanceId,
            Long codigo,
            StatusServicoFarmaceutico status,
            LocalDateTime createdAt
    ) {
    }
}

package org.fen.fen.servicofarmaceutico.dto;

import org.fen.fen.domain.StatusServicoFarmaceutico;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public record ServicoFarmaceuticoResumoResponse(UUID id, Long codigo, UUID patientId, String patientName,
        String patientCpf, LocalDate attendanceDate, StatusServicoFarmaceutico status,
        List<String> selectedServices, boolean canContinue, Integer nextReturnNumber,
        Integer returnCount, boolean editAllowed) {}

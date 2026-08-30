package org.fen.fen.servicofarmaceutico.dto;

import org.fen.fen.paciente.dto.PacienteResponse;

import java.util.UUID;

public record ContinuacaoResponse(UUID previousAttendanceId, Long previousAttendanceCode,
        PacienteResponse patient, ServicoFarmaceuticoResponse.FollowUpProgress followUpProgress) {}

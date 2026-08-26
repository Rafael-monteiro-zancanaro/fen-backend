package org.fen.fen.paciente.dto;
import org.fen.fen.comorbidade.dto.ComorbidadeSummaryResponse;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
public record PacienteResponse(UUID id, String name, String cpf, LocalDate birthDate, String cellPhone,
        String gender, String cep, String address, String neighborhood, String city, String state,
        String phone, String responsibleName, List<UUID> comorbidityIds,
        List<ComorbidadeSummaryResponse> comorbidities, LocalDateTime createdAt, LocalDateTime updatedAt) {}

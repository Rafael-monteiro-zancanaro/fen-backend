package org.fen.fen.paciente.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public record PacienteRequest(
        @NotBlank @Size(max = 150) String name,
        @NotBlank @Pattern(regexp = "(?:\\d{11}|\\d{3}\\.\\d{3}\\.\\d{3}-\\d{2})") String cpf,
        @NotNull LocalDate birthDate,
        @NotBlank @Pattern(regexp = "\\D*(?:\\d\\D*){10,11}") String cellPhone,
        @Size(max = 20) String gender, @Pattern(regexp = "^$|(?:\\d{8}|\\d{5}-\\d{3})") String cep,
        @Size(max = 254) String address, @Size(max = 150) String neighborhood, @Size(max = 150) String city,
        @Pattern(regexp = "^$|[A-Za-z]{2}") String state, @Pattern(regexp = "^$|\\D*(?:\\d\\D*){10,11}") String phone,
        @Size(max = 150) String responsibleName, List<UUID> comorbidityIds) {
    public PacienteRequest { if (comorbidityIds == null) comorbidityIds = List.of(); }
}

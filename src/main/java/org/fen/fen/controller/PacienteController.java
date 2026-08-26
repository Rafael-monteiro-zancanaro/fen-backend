package org.fen.fen.controller;
import jakarta.validation.Valid; import lombok.RequiredArgsConstructor; import org.fen.fen.paciente.dto.*; import org.fen.fen.service.PacienteService; import org.springframework.data.domain.*; import org.springframework.data.web.PageableDefault; import org.springframework.http.HttpStatus; import org.springframework.web.bind.annotation.*; import java.util.UUID;
@RestController @RequiredArgsConstructor @RequestMapping("/api/pacientes") public class PacienteController {
 private final PacienteService service;
 @PostMapping @ResponseStatus(HttpStatus.CREATED) public PacienteResponse criar(@Valid @RequestBody PacienteRequest request){return service.criar(request);}
 @GetMapping public Page<PacienteResponse> listar(@RequestParam(defaultValue="") String query,@PageableDefault(size=10) Pageable pageable){return service.listar(query,pageable);}
 @GetMapping("/cpf/{cpf}") public PacienteResponse buscarCpf(@PathVariable String cpf){return service.buscarCpf(cpf);}
 @GetMapping("/{id}") public PacienteResponse buscar(@PathVariable UUID id){return service.buscar(id);}
 @PutMapping("/{id}") public PacienteResponse atualizar(@PathVariable UUID id,@Valid @RequestBody PacienteRequest request){return service.atualizar(id,request);}
}

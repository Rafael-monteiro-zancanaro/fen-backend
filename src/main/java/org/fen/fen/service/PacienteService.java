package org.fen.fen.service;

import lombok.RequiredArgsConstructor;
import org.fen.fen.comorbidade.dto.ComorbidadeSummaryResponse;
import org.fen.fen.domain.Comorbidade;
import org.fen.fen.domain.Paciente;
import org.fen.fen.error.ConflictException;
import org.fen.fen.paciente.dto.PacienteRequest;
import org.fen.fen.paciente.dto.PacienteResponse;
import org.fen.fen.repository.ComorbidadeRepository;
import org.fen.fen.repository.PacienteRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.*;

@Service @RequiredArgsConstructor
public class PacienteService {
    private final PacienteRepository repository; private final ComorbidadeRepository comorbidadeRepository;
    @Transactional public PacienteResponse criar(PacienteRequest request) {
        String cpf = digitos(request.cpf()); if (repository.existsByCpf(cpf)) throw new ConflictException("CPF já cadastrado");
        Paciente paciente = new Paciente(); preencher(paciente, request, cpf); return resposta(repository.saveAndFlush(paciente));
    }
    @Transactional(readOnly = true) public PacienteResponse buscar(UUID id) { return resposta(encontrar(id)); }
    @Transactional(readOnly = true) public PacienteResponse buscarCpf(String cpf) { return resposta(repository.findByCpf(digitos(cpf)).orElseThrow(() -> new NoSuchElementException("Paciente não encontrado"))); }
    @Transactional(readOnly = true) public Page<PacienteResponse> listar(String query, Pageable pageable) {
        String valor = query == null ? "" : query.trim(); return repository.buscar(valor, digitos(valor), pageable).map(this::resposta);
    }
    @Transactional public PacienteResponse atualizar(UUID id, PacienteRequest request) {
        Paciente paciente = encontrar(id); String cpf = digitos(request.cpf());
        if (!paciente.getCpf().equals(cpf) && repository.existsByCpf(cpf)) throw new ConflictException("CPF já cadastrado");
        preencher(paciente, request, cpf); return resposta(repository.saveAndFlush(paciente));
    }
    private Paciente encontrar(UUID id) { return repository.buscarDetalhe(id).orElseThrow(() -> new NoSuchElementException("Paciente não encontrado")); }
    private void preencher(Paciente p, PacienteRequest r, String cpf) {
        p.setNome(r.name().trim()); p.setCpf(cpf); p.setDataNascimento(r.birthDate()); p.setCelular(digitos(r.cellPhone()));
        p.setGenero(limpar(r.gender())); p.setCep(digitos(r.cep())); p.setEndereco(limpar(r.address())); p.setBairro(limpar(r.neighborhood())); p.setCidade(limpar(r.city())); p.setEstado(limpar(r.state()).toUpperCase(Locale.ROOT)); p.setTelefone(digitos(r.phone())); p.setNomeResponsavel(limpar(r.responsibleName()));
        Set<UUID> ids = new LinkedHashSet<>(r.comorbidityIds()); if (ids.size() != r.comorbidityIds().size()) throw new IllegalArgumentException("Comorbidades não podem ser repetidas");
        List<Comorbidade> comorbidades = comorbidadeRepository.findAllById(ids); if (comorbidades.size() != ids.size()) throw new NoSuchElementException("Uma ou mais comorbidades não foram encontradas");
        p.setComorbidades(new LinkedHashSet<>(comorbidades));
    }
    private PacienteResponse resposta(Paciente p) { var itens = p.getComorbidades().stream().sorted(Comparator.comparing(Comorbidade::getNome)).map(c -> new ComorbidadeSummaryResponse(c.getId(), c.getNome(), c.getInteracoes().size(), c.getCreatedAt())).toList(); return new PacienteResponse(p.getId(),p.getNome(),p.getCpf(),p.getDataNascimento(),p.getCelular(),p.getGenero(),p.getCep(),p.getEndereco(),p.getBairro(),p.getCidade(),p.getEstado(),p.getTelefone(),p.getNomeResponsavel(),itens.stream().map(ComorbidadeSummaryResponse::id).toList(),itens,p.getCreatedAt(),p.getUpdatedAt()); }
    private String digitos(String value) { return value == null ? "" : value.replaceAll("\\D", ""); }
    private String limpar(String value) { return value == null ? "" : value.trim(); }
}

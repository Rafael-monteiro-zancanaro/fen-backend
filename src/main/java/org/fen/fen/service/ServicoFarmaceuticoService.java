package org.fen.fen.service;

import lombok.RequiredArgsConstructor;
import org.fen.fen.domain.Acompanhamento;
import org.fen.fen.domain.CuidadosFarmaceuticos;
import org.fen.fen.domain.DadosServicosFarmaceuticos;
import org.fen.fen.domain.Medicamento;
import org.fen.fen.domain.MedicamentoAtendimento;
import org.fen.fen.domain.Paciente;
import org.fen.fen.domain.ServicoFarmaceutico;
import org.fen.fen.domain.StatusServicoFarmaceutico;
import org.fen.fen.domain.TipoServicoMedicamento;
import org.fen.fen.error.BusinessRuleException;
import org.fen.fen.error.ConflictException;
import org.fen.fen.mapper.ServicoFarmaceuticoMapper;
import org.fen.fen.paciente.dto.PacienteRequest;
import org.fen.fen.paciente.dto.PacienteResponse;
import org.fen.fen.repository.AcompanhamentoRepository;
import org.fen.fen.repository.MedicamentoRepository;
import org.fen.fen.repository.MedicamentoAtendimentoRepository;
import org.fen.fen.repository.PacienteRepository;
import org.fen.fen.repository.ServicoFarmaceuticoRepository;
import org.fen.fen.servicofarmaceutico.dto.ContinuacaoResponse;
import org.fen.fen.servicofarmaceutico.dto.MedicamentoAtendimentoRequest;
import org.fen.fen.servicofarmaceutico.dto.ServicoFarmaceuticoRequest;
import org.fen.fen.servicofarmaceutico.dto.ServicoFarmaceuticoResponse;
import org.fen.fen.servicofarmaceutico.dto.ServicoFarmaceuticoResumoResponse;
import org.fen.fen.servicofarmaceutico.dto.ServicoFarmaceuticoBuscaAvancadaResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ServicoFarmaceuticoService {

    private final ServicoFarmaceuticoRepository repository;
    private final AcompanhamentoRepository acompanhamentoRepository;
    private final PacienteRepository pacienteRepository;
    private final MedicamentoRepository medicamentoRepository;
    private final MedicamentoAtendimentoRepository medicamentoAtendimentoRepository;
    private final PacienteService pacienteService;
    private final StatusEfetivoResolver statusResolver;
    private final JdbcTemplate jdbcTemplate;
    private final Clock clock;

    @Transactional
    public ServicoFarmaceuticoResponse criar(ServicoFarmaceuticoRequest request) {
        Paciente paciente = resolverPacienteParaCriacao(request);
        ServicoFarmaceutico atendimento = novoAtendimento(paciente, request, null, 0);

        if (request.followUp() != null) {
            criarAcompanhamento(atendimento, request.followUp());
        }

        return resposta(atendimento);
    }

    @Transactional(readOnly = true)
    public ServicoFarmaceuticoResponse buscar(UUID id) {
        return resposta(encontrar(id));
    }

    @Transactional(readOnly = true)
    public Page<ServicoFarmaceuticoResumoResponse> listar(String query, String status, Pageable pageable) {
        String termo = query == null ? "" : query.trim();
        String digitos = termo.replaceAll("\\D", "");
        String filtro = status == null || status.equals("TODOS") ? "" : status;

        return repository.listar(termo, digitos, filtro, LocalDate.now(clock), pageable)
                .map(this::resumo);
    }

    @Transactional(readOnly = true)
    public Page<ServicoFarmaceuticoBuscaAvancadaResponse> buscarAvancado(
            String cpf,
            UUID medicamentoId,
            String lote,
            LocalDate dataAtendimento,
            Pageable pageable
    ) {
        String cpfNormalizado = cpf == null ? "" : cpf.replaceAll("\\D", "");
        String loteNormalizado = lote == null ? "" : lote.trim();
        Page<ServicoFarmaceutico> pagina = repository.buscarAvancado(
                cpfNormalizado,
                medicamentoId,
                loteNormalizado,
                dataAtendimento,
                pageable
        );
        Map<UUID, List<MedicamentoAtendimento>> itensPorAtendimento = medicamentoAtendimentoRepository
                .findByServicoFarmaceuticoIdIn(pagina.getContent().stream().map(ServicoFarmaceutico::getId).toList())
                .stream()
                .filter(item -> corresponde(item, medicamentoId, loteNormalizado))
                .collect(java.util.stream.Collectors.groupingBy(
                        item -> item.getServicoFarmaceutico().getId()
                ));

        return pagina.map(atendimento -> new ServicoFarmaceuticoBuscaAvancadaResponse(
                resumo(atendimento),
                itensPorAtendimento.getOrDefault(atendimento.getId(), List.of()).stream()
                        .map(ServicoFarmaceuticoMapper::medication)
                        .toList()
        ));
    }

    @Transactional
    public ServicoFarmaceuticoResponse atualizar(UUID id, ServicoFarmaceuticoRequest request) {
        ServicoFarmaceutico atendimento = encontrar(id);
        validarPacienteDaEdicao(atendimento, request);
        validarAcompanhamentoDaEdicao(atendimento, request);
        preencher(atendimento, request);

        if (atendimento.getAcompanhamento() == null && request.followUp() != null) {
            criarAcompanhamento(atendimento, request.followUp());
        } else if (atendimento.getAcompanhamento() != null && request.followUp() != null) {
            atualizarAcompanhamento(atendimento.getAcompanhamento(), request.followUp());
        }

        repository.saveAndFlush(atendimento);
        return resposta(atendimento);
    }

    @Transactional(readOnly = true)
    public ContinuacaoResponse buscarContinuacao(UUID id) {
        ServicoFarmaceutico atendimento = encontrar(id);
        validarPodeProsseguir(atendimento);

        return new ContinuacaoResponse(
                atendimento.getId(),
                atendimento.getCodigo(),
                pacienteService.buscar(atendimento.getPaciente().getId()),
                progresso(atendimento)
        );
    }

    @Transactional
    public ServicoFarmaceuticoResponse criarRetorno(UUID anteriorId, ServicoFarmaceuticoRequest request) {
        Acompanhamento acompanhamento = acompanhamentoRepository.buscarPorAtendimentoAtualComLock(anteriorId)
                .orElseThrow(() -> new ConflictException("Atendimento não pode prosseguir"));
        ServicoFarmaceutico anterior = acompanhamento.getAtendimentoAtual();

        validarPodeProsseguir(anterior);
        validarPacienteDoRetorno(anterior, request);

        int proximaPosicao = anterior.getNumeroRetorno() + 1;
        anterior.setStatusPersistido(StatusServicoFarmaceutico.CONCLUIDO);

        ServicoFarmaceutico retorno = novoAtendimento(
                anterior.getPaciente(),
                request,
                acompanhamento,
                proximaPosicao
        );
        retorno.setAtendimentoAnterior(anterior);

        boolean ultimoRetorno = proximaPosicao == acompanhamento.getQuantidadeRetornos();
        retorno.setStatusPersistido(ultimoRetorno
                ? StatusServicoFarmaceutico.CONCLUIDO
                : StatusServicoFarmaceutico.AGUARDANDO_RETORNO);
        acompanhamento.setAtendimentoAtual(retorno);

        if (ultimoRetorno) {
            acompanhamento.setEncerradoEm(LocalDate.now(clock));
        } else {
            acompanhamento.setProximoRetorno(
                    retorno.getDataAtendimento().plusDays(acompanhamento.getIntervaloRetornoDias())
            );
        }

        return resposta(retorno);
    }

    @Transactional
    public ServicoFarmaceuticoResponse encerrar(UUID id) {
        Acompanhamento acompanhamento = acompanhamentoRepository.buscarPorAtendimentoAtualComLock(id)
                .orElseThrow(() -> new BusinessRuleException("Atendimento não possui acompanhamento ativo"));
        ServicoFarmaceutico atendimento = acompanhamento.getAtendimentoAtual();

        if (statusResolver.resolver(atendimento) != StatusServicoFarmaceutico.EXPIRADO) {
            throw new BusinessRuleException("Somente atendimento expirado pode ser encerrado");
        }

        atendimento.setStatusPersistido(StatusServicoFarmaceutico.CONCLUIDO);
        acompanhamento.setEncerradoEm(LocalDate.now(clock));
        return resposta(atendimento);
    }

    private ServicoFarmaceutico novoAtendimento(
            Paciente paciente,
            ServicoFarmaceuticoRequest request,
            Acompanhamento acompanhamento,
            int numeroRetorno
    ) {
        if (acompanhamento != null && request.followUp() != null) {
            throw new BusinessRuleException("Retorno não pode criar acompanhamento");
        }

        ServicoFarmaceutico atendimento = new ServicoFarmaceutico();
        atendimento.setCodigo(proximoCodigo());
        atendimento.setPaciente(paciente);
        atendimento.setDataAtendimento(LocalDate.now(clock));
        atendimento.setAcompanhamento(acompanhamento);
        atendimento.setNumeroRetorno(numeroRetorno);
        atendimento.setStatusPersistido(acompanhamento != null || request.followUp() != null
                ? StatusServicoFarmaceutico.AGUARDANDO_RETORNO
                : StatusServicoFarmaceutico.CONCLUIDO);
        preencher(atendimento, request);

        return repository.saveAndFlush(atendimento);
    }

    private void criarAcompanhamento(
            ServicoFarmaceutico atendimento,
            ServicoFarmaceuticoRequest.FollowUp followUp
    ) {
        Acompanhamento acompanhamento = new Acompanhamento();
        acompanhamento.setAtendimentoInicial(atendimento);
        acompanhamento.setAtendimentoAtual(atendimento);
        aplicarConfiguracaoAcompanhamento(acompanhamento, atendimento, followUp);
        acompanhamentoRepository.saveAndFlush(acompanhamento);

        atendimento.setAcompanhamento(acompanhamento);
        atendimento.setStatusPersistido(StatusServicoFarmaceutico.AGUARDANDO_RETORNO);
        repository.saveAndFlush(atendimento);
    }

    private void atualizarAcompanhamento(
            Acompanhamento acompanhamento,
            ServicoFarmaceuticoRequest.FollowUp followUp
    ) {
        ServicoFarmaceutico atual = acompanhamento.getAtendimentoAtual();
        if (followUp.returnCount() < atual.getNumeroRetorno()) {
            throw new BusinessRuleException("Quantidade de retornos não pode ser menor que os retornos já realizados");
        }

        aplicarConfiguracaoAcompanhamento(acompanhamento, atual, followUp);
    }

    private void aplicarConfiguracaoAcompanhamento(
            Acompanhamento acompanhamento,
            ServicoFarmaceutico referencia,
            ServicoFarmaceuticoRequest.FollowUp followUp
    ) {
        acompanhamento.setIntervaloRetornoDias(followUp.returnIntervalDays());
        acompanhamento.setQuantidadeRetornos(followUp.returnCount());
        acompanhamento.setProximoRetorno(
                referencia.getDataAtendimento().plusDays(followUp.returnIntervalDays())
        );
    }

    private void preencher(ServicoFarmaceutico atendimento, ServicoFarmaceuticoRequest request) {
        atendimento.setCuidadosFarmaceuticos(request.care() == null ? null : cuidados(request.care()));
        atendimento.setDadosServicosFarmaceuticos(
                request.complementaryServices() == null ? null : complementar(request.complementaryServices())
        );
        sincronizarMedicamentos(atendimento, request);
    }

    private void sincronizarMedicamentos(ServicoFarmaceutico atendimento, ServicoFarmaceuticoRequest request) {
        List<ItemSolicitado> solicitados = new ArrayList<>();
        adicionarSolicitados(
                solicitados,
                request.injectable() == null ? List.of() : request.injectable().medications(),
                TipoServicoMedicamento.APLICACAO_INJETAVEIS
        );
        adicionarSolicitados(
                solicitados,
                request.inhalotherapy() == null ? List.of() : request.inhalotherapy().medications(),
                TipoServicoMedicamento.INALOTERAPIA
        );
        adicionarSolicitados(
                solicitados,
                request.complementaryServices() == null ? List.of() : request.complementaryServices().medications(),
                TipoServicoMedicamento.SERVICOS_FARMACEUTICOS
        );

        Set<UUID> medicationIds = solicitados.stream()
                .map(item -> item.request().medicationId())
                .collect(java.util.stream.Collectors.toSet());
        Map<UUID, Medicamento> medicamentos = buscarMedicamentos(medicationIds);
        Map<UUID, MedicamentoAtendimento> existentes = atendimento.getMedicamentosAtendimento().stream()
                .filter(item -> item.getId() != null)
                .collect(java.util.stream.Collectors.toMap(MedicamentoAtendimento::getId, item -> item));
        Set<UUID> idsMantidos = new HashSet<>();

        for (ItemSolicitado solicitado : solicitados) {
            MedicamentoAtendimento item = solicitado.request().id() == null
                    ? new MedicamentoAtendimento()
                    : existentes.get(solicitado.request().id());
            if (item == null) {
                throw new BusinessRuleException("Medicamento do atendimento não pertence a este atendimento");
            }

            preencherMedicamento(item, solicitado, medicamentos.get(solicitado.request().medicationId()));
            if (item.getId() == null) {
                atendimento.adicionarMedicamento(item);
            } else {
                idsMantidos.add(item.getId());
            }
        }

        List<MedicamentoAtendimento> removidos = atendimento.getMedicamentosAtendimento().stream()
                .filter(item -> item.getId() != null && !idsMantidos.contains(item.getId()))
                .toList();
        removidos.forEach(atendimento::removerMedicamento);
    }

    private void adicionarSolicitados(
            List<ItemSolicitado> destino,
            List<MedicamentoAtendimentoRequest> itens,
            TipoServicoMedicamento tipo
    ) {
        itens.forEach(item -> destino.add(new ItemSolicitado(item, tipo)));
    }

    private Map<UUID, Medicamento> buscarMedicamentos(Set<UUID> ids) {
        Map<UUID, Medicamento> medicamentos = new HashMap<>();
        medicamentoRepository.findAllById(ids).forEach(medicamento -> medicamentos.put(medicamento.getId(), medicamento));
        if (medicamentos.size() != ids.size()) {
            throw new NoSuchElementException("Um ou mais medicamentos não foram encontrados");
        }
        return medicamentos;
    }

    private void preencherMedicamento(
            MedicamentoAtendimento item,
            ItemSolicitado solicitado,
            Medicamento medicamento
    ) {
        MedicamentoAtendimentoRequest request = solicitado.request();
        item.setMedicamento(medicamento);
        item.setTipoServico(solicitado.tipo());
        item.setLote(request.batch().trim());
        item.setValidade(request.expirationDate());
        item.setPosologia(request.dosage().trim());
        if (solicitado.tipo() == TipoServicoMedicamento.SERVICOS_FARMACEUTICOS) {
            item.setNomePrescritor(null);
            item.setRegistroPrescritor(null);
            return;
        }

        item.setNomePrescritor(normalizarOpcional(request.prescriberName()));
        item.setRegistroPrescritor(normalizarOpcional(request.prescriberRegistration()));
    }

    private boolean corresponde(MedicamentoAtendimento item, UUID medicamentoId, String lote) {
        return (medicamentoId == null || item.getMedicamento().getId().equals(medicamentoId))
                && (lote.isEmpty() || item.getLote().toLowerCase().contains(lote.toLowerCase()));
    }

    private Paciente resolverPacienteParaCriacao(ServicoFarmaceuticoRequest request) {
        if (request.patientId() != null) {
            return pacienteRepository.findById(request.patientId())
                    .orElseThrow(() -> new NoSuchElementException("Paciente não encontrado"));
        }

        PacienteRequest novoPaciente = request.patient();
        if (novoPaciente == null) {
            throw new BusinessRuleException("Informe o paciente do atendimento");
        }

        try {
            PacienteResponse criado = pacienteService.criar(novoPaciente);
            return pacienteRepository.findById(criado.id()).orElseThrow();
        } catch (ConflictException exception) {
            throw new ConflictException("CPF já cadastrado; consulte o paciente antes de salvar o atendimento");
        }
    }

    private void validarPacienteDaEdicao(ServicoFarmaceutico atendimento, ServicoFarmaceuticoRequest request) {
        if (request.patient() != null || request.patientId() == null
                || !request.patientId().equals(atendimento.getPaciente().getId())) {
            throw new BusinessRuleException("O paciente do atendimento não pode ser alterado");
        }
    }

    private void validarAcompanhamentoDaEdicao(ServicoFarmaceutico atendimento, ServicoFarmaceuticoRequest request) {
        if (atendimento.getAtendimentoAnterior() != null && request.followUp() != null) {
            throw new BusinessRuleException("Retorno não pode criar ou alterar acompanhamento");
        }
    }

    private void validarPacienteDoRetorno(ServicoFarmaceutico anterior, ServicoFarmaceuticoRequest request) {
        if (request.patient() != null || request.patientId() == null
                || !request.patientId().equals(anterior.getPaciente().getId())) {
            throw new BusinessRuleException("Retorno deve usar o paciente do acompanhamento");
        }
    }

    private void validarPodeProsseguir(ServicoFarmaceutico atendimento) {
        Acompanhamento acompanhamento = atendimento.getAcompanhamento();
        if (acompanhamento == null
                || acompanhamento.getAtendimentoAtual() == null
                || !acompanhamento.getAtendimentoAtual().getId().equals(atendimento.getId())
                || atendimento.getNumeroRetorno() >= acompanhamento.getQuantidadeRetornos()
                || acompanhamento.getEncerradoEm() != null
                || statusResolver.resolver(atendimento) == StatusServicoFarmaceutico.CONCLUIDO) {
            throw new ConflictException("Atendimento não pode prosseguir");
        }
    }

    private Long proximoCodigo() {
        return jdbcTemplate.queryForObject("select nextval('servicofarmaceutico_codigo_seq')", Long.class);
    }

    private ServicoFarmaceutico encontrar(UUID id) {
        return repository.buscarDetalhe(id)
                .orElseThrow(() -> new NoSuchElementException("Atendimento não encontrado"));
    }

    private CuidadosFarmaceuticos cuidados(ServicoFarmaceuticoRequest.Care request) {
        CuidadosFarmaceuticos cuidados = new CuidadosFarmaceuticos();
        cuidados.setGlicemiaCapilar(request.bloodGlucose());
        cuidados.setPressaoSistolica(request.systolicPressure());
        cuidados.setPressaoDiastolica(request.diastolicPressure());
        cuidados.setTemperaturaCorporal(request.bodyTemperature());
        return cuidados;
    }

    private DadosServicosFarmaceuticos complementar(ServicoFarmaceuticoRequest.ComplementaryServices request) {
        DadosServicosFarmaceuticos servicos = new DadosServicosFarmaceuticos();
        servicos.setAssistenciaDomiciliar(request.homeCare());
        servicos.setAcompanhamentoFarmacoterapeutico(request.pharmacotherapeuticFollowUp());
        servicos.setIndicacaoTranstornosMenores(request.minorDisorderIndication());
        servicos.setSinaisESintomas(request.signsAndSymptoms());
        return servicos;
    }

    private String normalizarOpcional(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private ServicoFarmaceuticoResponse.FollowUpProgress progresso(ServicoFarmaceutico atendimento) {
        Acompanhamento acompanhamento = atendimento.getAcompanhamento();
        if (acompanhamento == null) {
            return new ServicoFarmaceuticoResponse.FollowUpProgress(0, 0, null, false);
        }

        int retornosRealizados = atendimento.getNumeroRetorno();
        int quantidadeRetornos = acompanhamento.getQuantidadeRetornos();
        boolean podeProsseguir = retornosRealizados < quantidadeRetornos
                && acompanhamento.getEncerradoEm() == null
                && statusResolver.resolver(atendimento) != StatusServicoFarmaceutico.CONCLUIDO;
        return new ServicoFarmaceuticoResponse.FollowUpProgress(
                quantidadeRetornos,
                retornosRealizados,
                podeProsseguir ? retornosRealizados + 1 : null,
                podeProsseguir
        );
    }

    private ServicoFarmaceuticoResumoResponse resumo(ServicoFarmaceutico atendimento) {
        ServicoFarmaceuticoResponse.FollowUpProgress progresso = progresso(atendimento);
        return new ServicoFarmaceuticoResumoResponse(
                atendimento.getId(),
                atendimento.getCodigo(),
                atendimento.getPaciente().getId(),
                atendimento.getPaciente().getNome(),
                atendimento.getPaciente().getCpf(),
                atendimento.getDataAtendimento(),
                statusResolver.resolver(atendimento),
                servicosSelecionados(atendimento),
                progresso.canContinue(),
                progresso.nextReturnNumber(),
                progresso.returnCount(),
                true
        );
    }

    private ServicoFarmaceuticoResponse resposta(ServicoFarmaceutico atendimento) {
        Acompanhamento acompanhamento = atendimento.getAcompanhamento();
        List<ServicoFarmaceuticoResponse.FollowUpHistoryEntry> historico = acompanhamento == null
                ? List.of()
                : repository.findByAcompanhamentoIdOrderByNumeroRetornoAsc(acompanhamento.getId()).stream()
                        .map(this::historico)
                        .toList();

        return new ServicoFarmaceuticoResponse(
                atendimento.getId(),
                atendimento.getCodigo(),
                pacienteService.buscar(atendimento.getPaciente().getId()),
                servicosSelecionados(atendimento),
                atendimento.getDataAtendimento(),
                atendimento.getCreatedAt(),
                statusResolver.resolver(atendimento),
                ServicoFarmaceuticoMapper.care(atendimento),
                ServicoFarmaceuticoMapper.injectable(atendimento),
                ServicoFarmaceuticoMapper.inhalotherapy(atendimento),
                ServicoFarmaceuticoMapper.complementaryServices(atendimento),
                ServicoFarmaceuticoMapper.followUp(acompanhamento),
                linkAcompanhamento(atendimento),
                progresso(atendimento),
                historico,
                true
        );
    }

    private List<String> servicosSelecionados(ServicoFarmaceutico atendimento) {
        List<String> selecionados = new ArrayList<>();
        if (atendimento.getCuidadosFarmaceuticos() != null) {
            selecionados.add("cuidados-farmaceuticos");
        }
        if (temMedicamentos(atendimento, TipoServicoMedicamento.APLICACAO_INJETAVEIS)) {
            selecionados.add("aplicacao-injetaveis");
        }
        if (temMedicamentos(atendimento, TipoServicoMedicamento.INALOTERAPIA)) {
            selecionados.add("inaloterapia");
        }
        if (atendimento.getDadosServicosFarmaceuticos() != null) {
            selecionados.add("servicos-farmaceuticos");
        }
        return selecionados;
    }

    private ServicoFarmaceuticoResponse.FollowUpLink linkAcompanhamento(ServicoFarmaceutico atendimento) {
        Acompanhamento acompanhamento = atendimento.getAcompanhamento();
        if (acompanhamento == null) {
            return null;
        }

        return new ServicoFarmaceuticoResponse.FollowUpLink(
                acompanhamento.getId(),
                acompanhamento.getAtendimentoInicial().getId(),
                atendimento.getAtendimentoAnterior() == null ? null : atendimento.getAtendimentoAnterior().getId(),
                atendimento.getNumeroRetorno()
        );
    }

    private ServicoFarmaceuticoResponse.FollowUpHistoryEntry historico(ServicoFarmaceutico atendimento) {
        String label = atendimento.getNumeroRetorno() == 0
                ? "Atendimento inicial"
                : "Retorno " + atendimento.getNumeroRetorno();
        return new ServicoFarmaceuticoResponse.FollowUpHistoryEntry(
                label,
                atendimento.getId(),
                atendimento.getCodigo(),
                statusResolver.resolver(atendimento),
                atendimento.getCreatedAt()
        );
    }

    private record ItemSolicitado(
            MedicamentoAtendimentoRequest request,
            TipoServicoMedicamento tipo
    ) {
    }

    private boolean temMedicamentos(ServicoFarmaceutico atendimento, TipoServicoMedicamento tipo) {
        return atendimento.getMedicamentosAtendimento().stream()
                .anyMatch(item -> item.getTipoServico() == tipo);
    }
}

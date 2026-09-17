package org.fen.fen.service;

import lombok.RequiredArgsConstructor;
import org.fen.fen.attachment.AttachmentProperties;
import org.fen.fen.attachment.AttachmentStorage;
import org.fen.fen.domain.AnexoAtendimento;
import org.fen.fen.domain.ServicoFarmaceutico;
import org.fen.fen.error.BusinessRuleException;
import org.fen.fen.repository.AnexoAtendimentoRepository;
import org.fen.fen.repository.ServicoFarmaceuticoRepository;
import org.fen.fen.servicofarmaceutico.dto.AnexoAtendimentoResponse;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.multipart.MultipartFile;

import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AnexoAtendimentoService {

    private final AnexoAtendimentoRepository anexoRepository;
    private final ServicoFarmaceuticoRepository servicoRepository;
    private final AttachmentStorage storage;
    private final AttachmentProperties properties;

    @Transactional
    public AnexoAtendimentoResponse anexar(UUID atendimentoId, MultipartFile file) {
        ServicoFarmaceutico atendimento = buscarAtendimento(atendimentoId);
        String nomeOriginal = validarNome(file);
        validarArquivo(file);
        String contentType = contentType(file);
        String key = storage.storeInStaging(file);

        try {
            AnexoAtendimento anexo = new AnexoAtendimento();
            anexo.setServicoFarmaceutico(atendimento);
            anexo.setNomeOriginal(nomeOriginal);
            anexo.setChaveArmazenamento(key);
            anexo.setContentType(contentType);
            anexo.setTamanho(file.getSize());
            anexoRepository.saveAndFlush(anexo);
            storage.promote(key);
            removerConteudoSeTransacaoReverter(key);
            return resposta(anexo);
        } catch (RuntimeException exception) {
            storage.discard(key);
            throw exception;
        }
    }

    @Transactional(readOnly = true)
    public List<AnexoAtendimentoResponse> listar(UUID atendimentoId) {
        buscarAtendimento(atendimentoId);
        return anexoRepository.findByServicoFarmaceuticoIdOrderByCreatedAtDesc(atendimentoId).stream()
                .map(this::resposta)
                .toList();
    }

    @Transactional(readOnly = true)
    public ResponseEntity<Resource> baixar(UUID atendimentoId, UUID anexoId) {
        AnexoAtendimento anexo = buscarAnexo(atendimentoId, anexoId);
        Path arquivo = storage.load(anexo.getChaveArmazenamento());
        MediaType mediaType = mediaType(anexo.getContentType());
        return ResponseEntity.ok()
                .contentType(mediaType)
                .contentLength(anexo.getTamanho())
                .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.attachment()
                        .filename(anexo.getNomeOriginal(), StandardCharsets.UTF_8)
                        .build().toString())
                .header("X-Content-Type-Options", "nosniff")
                .body(new FileSystemResource(arquivo));
    }

    @Transactional
    public void remover(UUID atendimentoId, UUID anexoId) {
        AnexoAtendimento anexo = buscarAnexo(atendimentoId, anexoId);
        storage.delete(anexo.getChaveArmazenamento());
        anexoRepository.delete(anexo);
        anexoRepository.flush();
    }

    private void removerConteudoSeTransacaoReverter(String key) {
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCompletion(int status) {
                if (status != STATUS_COMMITTED) {
                    storage.discard(key);
                }
            }
        });
    }

    private ServicoFarmaceutico buscarAtendimento(UUID atendimentoId) {
        return servicoRepository.findById(atendimentoId)
                .orElseThrow(() -> new NoSuchElementException("Atendimento não encontrado"));
    }

    private AnexoAtendimento buscarAnexo(UUID atendimentoId, UUID anexoId) {
        return anexoRepository.findByIdAndServicoFarmaceuticoId(anexoId, atendimentoId)
                .orElseThrow(() -> new NoSuchElementException("Anexo não encontrado para este atendimento"));
    }

    private String validarNome(MultipartFile file) {
        String nome = file == null ? null : file.getOriginalFilename();
        if (nome == null || nome.isBlank() || nome.length() > 255 || nome.contains("/") || nome.contains("\\\\")
                || nome.chars().anyMatch(Character::isISOControl)) {
            throw new BusinessRuleException("Nome do arquivo inválido");
        }
        return nome.trim();
    }

    private void validarArquivo(MultipartFile file) {
        if (file.isEmpty() || file.getSize() == 0) {
            throw new BusinessRuleException("Arquivo não pode estar vazio");
        }
        if (file.getSize() > properties.getMaxFileSize().toBytes()) {
            throw new BusinessRuleException("Arquivo excede o limite de " + properties.getMaxFileSize().toMegabytes() + " MB");
        }
    }

    private String contentType(MultipartFile file) {
        String informado = file.getContentType();
        if (informado == null || informado.isBlank()) {
            return MediaType.APPLICATION_OCTET_STREAM_VALUE;
        }
        try {
            return MediaType.parseMediaType(informado).toString();
        } catch (IllegalArgumentException exception) {
            throw new BusinessRuleException("Tipo de arquivo inválido");
        }
    }

    private MediaType mediaType(String value) {
        try {
            return MediaType.parseMediaType(value);
        } catch (IllegalArgumentException exception) {
            return MediaType.APPLICATION_OCTET_STREAM;
        }
    }

    private AnexoAtendimentoResponse resposta(AnexoAtendimento anexo) {
        return new AnexoAtendimentoResponse(
                anexo.getId(), anexo.getNomeOriginal(), anexo.getContentType(), anexo.getTamanho(), anexo.getCreatedAt()
        );
    }
}

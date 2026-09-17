package org.fen.fen.attachment;

import jakarta.annotation.PostConstruct;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.UUID;

@Component
public class AttachmentStorage {

    private final Path root;
    private final Path staging;

    public AttachmentStorage(AttachmentProperties properties) {
        if (properties.getStoragePath() == null) {
            throw new IllegalStateException("Diretório de armazenamento de anexos não configurado");
        }
        this.root = properties.getStoragePath().toAbsolutePath().normalize();
        this.staging = this.root.resolve(".staging").normalize();
    }

    @PostConstruct
    void initialize() {
        try {
            Files.createDirectories(root);
            Files.createDirectories(staging);
        } catch (IOException exception) {
            throw new IllegalStateException("Não foi possível preparar o armazenamento de anexos", exception);
        }
    }

    public String storeInStaging(MultipartFile file) {
        String key = UUID.randomUUID().toString();
        Path target = stagingPath(key);
        try (InputStream input = file.getInputStream()) {
            Files.copy(input, target, StandardCopyOption.REPLACE_EXISTING);
            return key;
        } catch (IOException exception) {
            deleteIfExists(target);
            throw new IllegalStateException("Não foi possível armazenar o anexo", exception);
        }
    }

    public void promote(String key) {
        try {
            Files.move(stagingPath(key), finalPath(key), StandardCopyOption.ATOMIC_MOVE);
        } catch (IOException exception) {
            throw new IllegalStateException("Não foi possível concluir o armazenamento do anexo", exception);
        }
    }

    public Path load(String key) {
        Path file = finalPath(key);
        if (!Files.isRegularFile(file)) {
            throw new IllegalStateException("Conteúdo do anexo não encontrado");
        }
        return file;
    }

    public void delete(String key) {
        Path file = finalPath(key);
        try {
            if (!Files.deleteIfExists(file)) {
                throw new IllegalStateException("Conteúdo do anexo não encontrado");
            }
        } catch (IOException exception) {
            throw new IllegalStateException("Não foi possível remover o conteúdo do anexo", exception);
        }
    }

    public void discard(String key) {
        deleteIfExists(stagingPath(key));
        deleteIfExists(finalPath(key));
    }

    private Path stagingPath(String key) {
        return resolve(staging, key);
    }

    private Path finalPath(String key) {
        return resolve(root, key);
    }

    private Path resolve(Path base, String key) {
        if (!key.matches("[0-9a-fA-F-]{36}")) {
            throw new IllegalArgumentException("Chave de armazenamento inválida");
        }
        Path resolved = base.resolve(key).normalize();
        if (!resolved.startsWith(base)) {
            throw new IllegalArgumentException("Chave de armazenamento inválida");
        }
        return resolved;
    }

    private void deleteIfExists(Path file) {
        try {
            Files.deleteIfExists(file);
        } catch (IOException ignored) {
            // A operação original preserva sua falha; esta é somente compensação de staging.
        }
    }
}

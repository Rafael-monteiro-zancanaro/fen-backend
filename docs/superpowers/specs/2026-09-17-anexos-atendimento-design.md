# Anexos de atendimento — design

## Objetivo

Permitir anexos independentes para cada `ServicoFarmaceutico`, administrados somente na visualização web do atendimento, sem participação no PDF.

## Armazenamento e domínio

Não há armazenamento de arquivos existente. O backend usará filesystem configurável por `fen.attachments.storage-path`, com fallback fora do repositório em `${java.io.tmpdir}/fen-attachments`; ambientes persistentes devem informar `FEN_ATTACHMENTS_STORAGE_PATH` apontando para volume persistente. O banco armazena apenas os metadados em `anexoatendimento`.

`AnexoAtendimento` é entidade auditada e possui UUID, atendimento proprietário, nome original, chave física UUID, content type, tamanho e a auditoria herdada. O nome original não compõe paths. A chave é validada e resolvida sob a raiz normalizada do storage.

## API e segurança

Todos os endpoints ficam sob `/api/servicos-farmaceuticos/{atendimentoId}/anexos` e recebem a proteção JWT existente. Download e remoção localizam por `anexoId` e `atendimentoId` conjuntamente. O download usa `Content-Disposition: attachment`, nome codificado em UTF-8 e `X-Content-Type-Options: nosniff`.

O limite por arquivo é 10 MB, centralizado em `fen.attachments.max-file-size`, com o limite de multipart Spring coerente. Não haverá whitelist de extensões sem requisito de domínio; arquivos vazios, nomes sem conteúdo ou com controles e content type malformado serão rejeitados. O fallback de tipo é `application/octet-stream`.

O upload grava em staging de chave UUID, persiste e força os metadados, promove para a chave definitiva e remove o conteúdo caso qualquer etapa posterior falhe. Na exclusão, o metadado só é removido após o arquivo ser removido; falha física aborta a transação. Assim não se confirmam metadados apontando para arquivo ausente nem uploads parcialmente persistidos em falhas previsíveis.

## Listagem e frontend

A lista de atendimentos recebe `attachmentCount`. Após buscar uma página de atendimentos, o backend executa uma única query agregada pelos IDs da página e mapeia a contagem, sem N+1. A tela de detalhe busca metadados por endpoint próprio e envia os arquivos de uma seleção múltipla individualmente, permitindo comunicar sucesso ou falha por arquivo. Sinais atualizam lista e contador após upload/delete.

Criação, edição e retorno navegam apenas após a resposta HTTP bem-sucedida para `/atendimentos/{id}`. Anexos não são enviados no request de atendimento, não são recriados em edição e não acompanham retornos. O DTO de impressão permanece sem anexos.

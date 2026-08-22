INSERT INTO usuario (id, email, passwordhash, role, situacao, createdat, updatedat, createdby, updatedby)
VALUES
    ('00000000-0000-0000-0000-000000000101', 'farma@fen.br', 'hash', 'FARMACEUTICO', 'ATIVO', TIMESTAMP '2026-08-21 08:00:00', NULL, 'Sistema', NULL),
    ('00000000-0000-0000-0000-000000000102', 'pendente@fen.br', 'hash', 'FARMACEUTICO', 'PENDENTE', TIMESTAMP '2026-08-21 09:00:00', NULL, 'Sistema', NULL),
    ('00000000-0000-0000-0000-000000000103', 'estagiaria@fen.br', 'hash', 'ESTAGIARIO', 'ATIVO', TIMESTAMP '2026-08-21 10:00:00', NULL, 'Sistema', NULL),
    ('00000000-0000-0000-0000-000000000104', 'admin@fen.br', 'hash', 'ADMIN', 'ATIVO', TIMESTAMP '2026-08-21 11:00:00', NULL, 'Sistema', NULL);

INSERT INTO funcionario (id, usuarioid, nome, cpf, datanascimento, crf, responsaveltecnico,
                         tipoestagio, supervisorid, iniciovigencia, fimvigencia,
                         createdat, updatedat, createdby, updatedby)
VALUES
    ('00000000-0000-0000-0000-000000000201', '00000000-0000-0000-0000-000000000101', 'Farmacêutica Ativa', '11111111111', NULL, 'PR-12345', TRUE, NULL, NULL, NULL, NULL, TIMESTAMP '2026-08-21 08:00:00', NULL, 'Sistema', NULL),
    ('00000000-0000-0000-0000-000000000202', '00000000-0000-0000-0000-000000000102', 'Farmacêutica Pendente', '22222222222', NULL, 'PR-23456', FALSE, NULL, NULL, NULL, NULL, TIMESTAMP '2026-08-21 09:00:00', NULL, 'Sistema', NULL),
    ('00000000-0000-0000-0000-000000000203', '00000000-0000-0000-0000-000000000103', 'Estagiária Ativa', '33333333333', NULL, NULL, NULL, 'OBRIGATORIO', '00000000-0000-0000-0000-000000000201', DATE '2026-08-01', DATE '2026-12-20', TIMESTAMP '2026-08-21 10:00:00', NULL, 'Sistema', NULL);

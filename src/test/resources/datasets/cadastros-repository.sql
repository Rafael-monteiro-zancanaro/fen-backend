INSERT INTO medicamento (id, nome, unidademedida, viaadministracao, createdat, createdby)
VALUES ('550e8400-e29b-41d4-a716-446655440001', 'Paracetamol', 'mg', 'Oral', CURRENT_TIMESTAMP, 'Sistema');

INSERT INTO comorbidade (id, nome, createdat, createdby)
VALUES ('0b0f9147-3675-401b-93c1-4d26f24b7d66', 'Diabetes', CURRENT_TIMESTAMP, 'Sistema');

INSERT INTO interacao (id, comorbidadeid, medicamentoid, createdat, createdby)
VALUES ('7fcbf136-004c-4069-97f1-dff9d3587259',
        '0b0f9147-3675-401b-93c1-4d26f24b7d66',
        '550e8400-e29b-41d4-a716-446655440001', CURRENT_TIMESTAMP, 'Sistema');

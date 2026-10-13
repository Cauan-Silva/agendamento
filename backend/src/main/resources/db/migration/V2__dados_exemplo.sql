-- Dados de exemplo para testar a API logo depois de subir o projeto.
-- Pode apagar este arquivo quando tiver telas de cadastro.

INSERT INTO servico (nome, duracao_minutos, preco) VALUES
    ('Corte de cabelo', 30, 45.00),
    ('Barba', 30, 35.00),
    ('Corte + barba', 60, 70.00);

INSERT INTO profissional (nome, hora_inicio, hora_fim) VALUES
    ('Ana', '09:00', '18:00'),
    ('Bruno', '13:00', '20:00');

INSERT INTO profissional_dia (profissional_id, dia) VALUES
    (1, 'MONDAY'), (1, 'TUESDAY'), (1, 'WEDNESDAY'), (1, 'THURSDAY'), (1, 'FRIDAY'),
    (2, 'TUESDAY'), (2, 'THURSDAY'), (2, 'SATURDAY');

INSERT INTO profissional_servico (profissional_id, servico_id) VALUES
    (1, 1), (1, 2), (1, 3),
    (2, 1), (2, 3);

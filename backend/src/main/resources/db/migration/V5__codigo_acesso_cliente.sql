-- Código secreto de cada agendamento: vai no link do e-mail para o cliente
-- consultar, cancelar ou remarcar o próprio horário sem precisar de login.
ALTER TABLE agendamento ADD COLUMN codigo_acesso VARCHAR(64);

UPDATE agendamento
SET codigo_acesso = md5(random()::text || id::text || clock_timestamp()::text)
                 || md5(random()::text || clock_timestamp()::text || id::text);

ALTER TABLE agendamento ALTER COLUMN codigo_acesso SET NOT NULL;
CREATE UNIQUE INDEX uk_agendamento_codigo_acesso ON agendamento (codigo_acesso);

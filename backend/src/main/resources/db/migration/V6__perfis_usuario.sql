-- Cada pessoa da equipe tem o próprio login.
-- ADMIN: dono/gerente, vê e altera tudo.
-- PROFISSIONAL: vê e atualiza só a própria agenda (precisa estar ligado a um profissional).
ALTER TABLE usuario ADD COLUMN perfil VARCHAR(20) NOT NULL DEFAULT 'ADMIN';
ALTER TABLE usuario ALTER COLUMN perfil DROP DEFAULT;
ALTER TABLE usuario ADD CONSTRAINT ck_usuario_perfil CHECK (perfil IN ('ADMIN', 'PROFISSIONAL'));

ALTER TABLE usuario ADD COLUMN profissional_id BIGINT REFERENCES profissional (id);
-- Um profissional tem no máximo um login
CREATE UNIQUE INDEX uk_usuario_profissional ON usuario (profissional_id) WHERE profissional_id IS NOT NULL;

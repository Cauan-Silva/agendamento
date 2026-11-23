-- Trocar ou redefinir a senha aumenta a versão; tokens emitidos com a versão anterior
-- deixam de valer na hora (veja ConversorJwt).
ALTER TABLE usuario ADD COLUMN versao_token INTEGER NOT NULL DEFAULT 0;

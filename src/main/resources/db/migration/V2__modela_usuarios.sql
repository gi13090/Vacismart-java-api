CREATE TABLE usuario (
    id uuid PRIMARY KEY,
    nome varchar(150) NOT NULL,
    email varchar(254) NOT NULL,
    senha_hash varchar(100) NOT NULL,
    perfil varchar(20) NOT NULL,
    unidade_saude_id uuid REFERENCES unidade_saude(id),
    ativo boolean NOT NULL DEFAULT true,
    CONSTRAINT uk_usuario_email UNIQUE (email),
    CONSTRAINT ck_usuario_email CHECK (email = lower(trim(email))),
    CONSTRAINT ck_usuario_perfil CHECK (perfil IN ('CIDADAO', 'FUNCIONARIO', 'GESTOR')),
    CONSTRAINT ck_usuario_vinculo CHECK (
        (perfil = 'FUNCIONARIO' AND unidade_saude_id IS NOT NULL) OR
        (perfil <> 'FUNCIONARIO' AND unidade_saude_id IS NULL))
);
CREATE INDEX ix_usuario_unidade ON usuario(unidade_saude_id);

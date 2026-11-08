-- J2: esquema sem seed; V0 permanece inalterada.
CREATE TABLE unidade_saude (
    id UUID PRIMARY KEY,
    nome VARCHAR(150) NOT NULL
);
CREATE TABLE vacina (
    id UUID PRIMARY KEY,
    nome VARCHAR(150) NOT NULL,
    temperatura_min_c NUMERIC(10,3) NOT NULL,
    temperatura_max_c NUMERIC(10,3) NOT NULL,
    margem_atencao_c NUMERIC(10,3) NOT NULL,
    CONSTRAINT ck_vacina_faixa CHECK (temperatura_min_c < temperatura_max_c
        AND margem_atencao_c >= 0 AND 2 * margem_atencao_c <= temperatura_max_c - temperatura_min_c)
);
CREATE TABLE transporte (
    id UUID PRIMARY KEY,
    codigo VARCHAR(60) NOT NULL UNIQUE,
    unidade_saude_id UUID NOT NULL REFERENCES unidade_saude(id),
    estado VARCHAR(40) NOT NULL CHECK (estado IN ('CADASTRADO','EM_TRANSPORTE','RECEBIDO','FINALIZADO')),
    inicio_monitoramento_em TIMESTAMP WITH TIME ZONE,
    CONSTRAINT ck_transporte_inicio CHECK (estado = 'CADASTRADO' OR inicio_monitoramento_em IS NOT NULL)
);
CREATE TABLE lote (
    id UUID PRIMARY KEY,
    codigo VARCHAR(60) NOT NULL UNIQUE,
    vacina_id UUID NOT NULL REFERENCES vacina(id),
    transporte_id UUID NOT NULL REFERENCES transporte(id),
    estado VARCHAR(40) NOT NULL CHECK (estado IN ('EM_MONITORAMENTO','AGUARDANDO_CONFERENCIA',
        'BLOQUEADO_PREVENTIVAMENTE','LIBERADO','DESCARTADO')),
    disponibilidade VARCHAR(30) NOT NULL CHECK (disponibilidade IN ('NAO_DISPONIVEL','CONFIRMADA'))
);
CREATE TABLE leitura_temperatura (
    id UUID PRIMARY KEY,
    transporte_id UUID NOT NULL REFERENCES transporte(id),
    dispositivo_id VARCHAR(100) NOT NULL,
    temperatura_c NUMERIC(10,3) NOT NULL,
    medida_em TIMESTAMP WITH TIME ZONE NOT NULL,
    recebida_em TIMESTAMP WITH TIME ZONE NOT NULL,
    classificacao VARCHAR(30) NOT NULL CHECK (classificacao IN ('NORMAL','ATENCAO','FORA_DA_FAIXA')),
    CONSTRAINT ck_leitura_tempos CHECK (medida_em <= recebida_em)
);
CREATE TABLE ocorrencia (
    id UUID PRIMARY KEY,
    transporte_id UUID NOT NULL REFERENCES transporte(id),
    tipo VARCHAR(30) NOT NULL CHECK (tipo IN ('FORA_DA_FAIXA','PERDA_DE_TELEMETRIA')),
    estado VARCHAR(20) NOT NULL CHECK (estado IN ('ABERTA','ENCERRADA')),
    aberta_em TIMESTAMP WITH TIME ZONE NOT NULL,
    encerrada_em TIMESTAMP WITH TIME ZONE,
    CONSTRAINT ck_ocorrencia_encerramento CHECK (
        (estado = 'ABERTA' AND encerrada_em IS NULL) OR
        (estado = 'ENCERRADA' AND encerrada_em IS NOT NULL AND encerrada_em >= aberta_em))
);
-- Defesa adicional no PostgreSQL; no H2 o fluxo é protegido pelo lock pessimista.
CREATE UNIQUE INDEX uk_ocorrencia_aberta ON ocorrencia(transporte_id, tipo) WHERE estado = 'ABERTA';
CREATE TABLE evento_auditoria (
    id UUID PRIMARY KEY,
    entidade VARCHAR(60) NOT NULL,
    referencia_id UUID NOT NULL,
    acao VARCHAR(100) NOT NULL,
    autor VARCHAR(100) NOT NULL,
    data_hora TIMESTAMP WITH TIME ZONE NOT NULL,
    justificativa VARCHAR(1000)
);
CREATE INDEX ix_transporte_unidade ON transporte(unidade_saude_id);
CREATE INDEX ix_lote_vacina ON lote(vacina_id);
CREATE INDEX ix_lote_transporte ON lote(transporte_id);
CREATE INDEX ix_leitura_transporte_recebida ON leitura_temperatura(transporte_id, recebida_em);
CREATE INDEX ix_auditoria_referencia ON evento_auditoria(referencia_id, data_hora);

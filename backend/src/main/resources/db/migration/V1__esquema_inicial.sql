-- MyBarber: esquema inicial (compativel com MySQL 5.7+ e 8.x)

CREATE TABLE tipo_cabelo (
    id          BIGINT AUTO_INCREMENT PRIMARY KEY,
    nome        VARCHAR(60)  NOT NULL,
    descricao   VARCHAR(255) NULL,
    ordem       INT          NOT NULL DEFAULT 0,
    ativo       TINYINT(1)   NOT NULL DEFAULT 1,
    CONSTRAINT uk_tipo_cabelo_nome UNIQUE (nome)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE barbearia (
    id                              BIGINT AUTO_INCREMENT PRIMARY KEY,
    nome                            VARCHAR(120)  NOT NULL,
    documento                       VARCHAR(20)   NULL,
    telefone                        VARCHAR(20)   NULL,
    email                           VARCHAR(160)  NULL,
    descricao                       VARCHAR(1000) NULL,
    cep                             VARCHAR(9)    NULL,
    logradouro                      VARCHAR(160)  NULL,
    numero                          VARCHAR(20)   NULL,
    complemento                     VARCHAR(80)   NULL,
    bairro                          VARCHAR(80)   NULL,
    cidade                          VARCHAR(80)   NULL,
    uf                              CHAR(2)       NULL,
    latitude                        DECIMAL(10,7) NULL,
    longitude                       DECIMAL(10,7) NULL,
    grade_minutos                   INT           NOT NULL DEFAULT 15,
    antecedencia_minima_min         INT           NOT NULL DEFAULT 30,
    prazo_cancelamento_min          INT           NOT NULL DEFAULT 120,
    prazo_resposta_remarcacao_horas INT           NOT NULL DEFAULT 12,
    status                          VARCHAR(20)   NOT NULL DEFAULT 'ATIVA',
    motivo_bloqueio                 VARCHAR(255)  NULL,
    bloqueio_automatico             TINYINT(1)    NOT NULL DEFAULT 0,
    valor_mensalidade               DECIMAL(10,2) NULL,
    dia_vencimento                  INT           NULL,
    dias_tolerancia                 INT           NOT NULL DEFAULT 5,
    criado_em                       DATETIME      NOT NULL,
    INDEX idx_barbearia_cidade (cidade),
    INDEX idx_barbearia_geo (latitude, longitude)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE usuario (
    id                 BIGINT AUTO_INCREMENT PRIMARY KEY,
    nome               VARCHAR(120) NOT NULL,
    email              VARCHAR(160) NOT NULL,
    senha_hash         VARCHAR(100) NOT NULL,
    telefone           VARCHAR(20)  NULL,
    papel              VARCHAR(20)  NOT NULL,
    ativo              TINYINT(1)   NOT NULL DEFAULT 1,
    barbearia_id       BIGINT       NULL,
    sexo               VARCHAR(20)  NULL,
    tipo_cabelo_id     BIGINT       NULL,
    comprimento_cabelo VARCHAR(20)  NULL,
    observacoes        VARCHAR(500) NULL,
    criado_em          DATETIME     NOT NULL,
    excluido_em        DATETIME     NULL,
    CONSTRAINT uk_usuario_email UNIQUE (email),
    CONSTRAINT fk_usuario_barbearia FOREIGN KEY (barbearia_id) REFERENCES barbearia (id),
    CONSTRAINT fk_usuario_tipo_cabelo FOREIGN KEY (tipo_cabelo_id) REFERENCES tipo_cabelo (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE refresh_token (
    id          BIGINT AUTO_INCREMENT PRIMARY KEY,
    usuario_id  BIGINT     NOT NULL,
    token_hash  CHAR(64)   NOT NULL,
    expira_em   DATETIME   NOT NULL,
    revogado    TINYINT(1) NOT NULL DEFAULT 0,
    criado_em   DATETIME   NOT NULL,
    CONSTRAINT uk_refresh_token_hash UNIQUE (token_hash),
    CONSTRAINT fk_refresh_usuario FOREIGN KEY (usuario_id) REFERENCES usuario (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE consentimento (
    id          BIGINT AUTO_INCREMENT PRIMARY KEY,
    usuario_id  BIGINT      NOT NULL,
    documento   VARCHAR(30) NOT NULL,
    versao      VARCHAR(20) NOT NULL,
    aceito_em   DATETIME    NOT NULL,
    ip          VARCHAR(45) NULL,
    CONSTRAINT fk_consentimento_usuario FOREIGN KEY (usuario_id) REFERENCES usuario (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE barbeiro (
    id            BIGINT AUTO_INCREMENT PRIMARY KEY,
    barbearia_id  BIGINT       NOT NULL,
    usuario_id    BIGINT       NULL,
    nome          VARCHAR(120) NOT NULL,
    telefone      VARCHAR(20)  NULL,
    bio           VARCHAR(500) NULL,
    intervalo_min INT          NOT NULL DEFAULT 0,
    ativo         TINYINT(1)   NOT NULL DEFAULT 1,
    criado_em     DATETIME     NOT NULL,
    CONSTRAINT uk_barbeiro_usuario UNIQUE (usuario_id),
    CONSTRAINT fk_barbeiro_barbearia FOREIGN KEY (barbearia_id) REFERENCES barbearia (id),
    CONSTRAINT fk_barbeiro_usuario FOREIGN KEY (usuario_id) REFERENCES usuario (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE servico (
    id            BIGINT AUTO_INCREMENT PRIMARY KEY,
    barbearia_id  BIGINT        NOT NULL,
    nome          VARCHAR(120)  NOT NULL,
    descricao     VARCHAR(500)  NULL,
    preco         DECIMAL(10,2) NOT NULL,
    duracao_min   INT           NOT NULL,
    ativo         TINYINT(1)    NOT NULL DEFAULT 1,
    criado_em     DATETIME      NOT NULL,
    CONSTRAINT fk_servico_barbearia FOREIGN KEY (barbearia_id) REFERENCES barbearia (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE servico_duracao (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    servico_id      BIGINT NOT NULL,
    tipo_cabelo_id  BIGINT NOT NULL,
    duracao_min     INT    NOT NULL,
    CONSTRAINT uk_servico_duracao UNIQUE (servico_id, tipo_cabelo_id),
    CONSTRAINT fk_servico_duracao_servico FOREIGN KEY (servico_id) REFERENCES servico (id) ON DELETE CASCADE,
    CONSTRAINT fk_servico_duracao_tipo FOREIGN KEY (tipo_cabelo_id) REFERENCES tipo_cabelo (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE barbeiro_tipo_cabelo (
    barbeiro_id     BIGINT NOT NULL,
    tipo_cabelo_id  BIGINT NOT NULL,
    PRIMARY KEY (barbeiro_id, tipo_cabelo_id),
    CONSTRAINT fk_btc_barbeiro FOREIGN KEY (barbeiro_id) REFERENCES barbeiro (id) ON DELETE CASCADE,
    CONSTRAINT fk_btc_tipo FOREIGN KEY (tipo_cabelo_id) REFERENCES tipo_cabelo (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE barbeiro_servico (
    barbeiro_id  BIGINT NOT NULL,
    servico_id   BIGINT NOT NULL,
    PRIMARY KEY (barbeiro_id, servico_id),
    CONSTRAINT fk_bs_barbeiro FOREIGN KEY (barbeiro_id) REFERENCES barbeiro (id) ON DELETE CASCADE,
    CONSTRAINT fk_bs_servico FOREIGN KEY (servico_id) REFERENCES servico (id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE horario_trabalho (
    id           BIGINT AUTO_INCREMENT PRIMARY KEY,
    barbeiro_id  BIGINT  NOT NULL,
    dia_semana   TINYINT NOT NULL,
    inicio       TIME    NOT NULL,
    fim          TIME    NOT NULL,
    INDEX idx_horario_barbeiro (barbeiro_id, dia_semana),
    CONSTRAINT fk_horario_barbeiro FOREIGN KEY (barbeiro_id) REFERENCES barbeiro (id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE horario_trabalho_servico (
    horario_id  BIGINT NOT NULL,
    servico_id  BIGINT NOT NULL,
    PRIMARY KEY (horario_id, servico_id),
    CONSTRAINT fk_hts_horario FOREIGN KEY (horario_id) REFERENCES horario_trabalho (id) ON DELETE CASCADE,
    CONSTRAINT fk_hts_servico FOREIGN KEY (servico_id) REFERENCES servico (id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE bloqueio (
    id           BIGINT AUTO_INCREMENT PRIMARY KEY,
    barbeiro_id  BIGINT       NOT NULL,
    inicio       DATETIME     NOT NULL,
    fim          DATETIME     NOT NULL,
    motivo       VARCHAR(255) NULL,
    INDEX idx_bloqueio_barbeiro (barbeiro_id, inicio),
    CONSTRAINT fk_bloqueio_barbeiro FOREIGN KEY (barbeiro_id) REFERENCES barbeiro (id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE agendamento (
    id                   BIGINT AUTO_INCREMENT PRIMARY KEY,
    barbearia_id         BIGINT        NOT NULL,
    barbeiro_id          BIGINT        NOT NULL,
    servico_id           BIGINT        NOT NULL,
    cliente_id           BIGINT        NULL,
    cliente_nome         VARCHAR(120)  NULL,
    cliente_telefone     VARCHAR(20)   NULL,
    tipo_cabelo_id       BIGINT        NULL,
    inicio               DATETIME      NOT NULL,
    fim                  DATETIME      NOT NULL,
    valor                DECIMAL(10,2) NOT NULL,
    status               VARCHAR(25)   NOT NULL,
    origem               VARCHAR(10)   NOT NULL,
    observacao           VARCHAR(500)  NULL,
    motivo_cancelamento  VARCHAR(255)  NULL,
    forma_pagamento      VARCHAR(20)   NULL,
    criado_em            DATETIME      NOT NULL,
    atualizado_em        DATETIME      NOT NULL,
    INDEX idx_agendamento_barbeiro (barbeiro_id, inicio),
    INDEX idx_agendamento_cliente (cliente_id, inicio),
    INDEX idx_agendamento_barbearia (barbearia_id, inicio),
    CONSTRAINT fk_agendamento_barbearia FOREIGN KEY (barbearia_id) REFERENCES barbearia (id),
    CONSTRAINT fk_agendamento_barbeiro FOREIGN KEY (barbeiro_id) REFERENCES barbeiro (id),
    CONSTRAINT fk_agendamento_servico FOREIGN KEY (servico_id) REFERENCES servico (id),
    CONSTRAINT fk_agendamento_cliente FOREIGN KEY (cliente_id) REFERENCES usuario (id),
    CONSTRAINT fk_agendamento_tipo FOREIGN KEY (tipo_cabelo_id) REFERENCES tipo_cabelo (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE remarcacao (
    id                BIGINT AUTO_INCREMENT PRIMARY KEY,
    agendamento_id    BIGINT       NOT NULL,
    inicio_anterior   DATETIME     NOT NULL,
    inicio_sugerido   DATETIME     NOT NULL,
    motivo            VARCHAR(255) NULL,
    status            VARCHAR(20)  NOT NULL,
    prazo             DATETIME     NOT NULL,
    criado_em         DATETIME     NOT NULL,
    respondido_em     DATETIME     NULL,
    INDEX idx_remarcacao_status (status, prazo),
    CONSTRAINT fk_remarcacao_agendamento FOREIGN KEY (agendamento_id) REFERENCES agendamento (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE notificacao (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    usuario_id      BIGINT       NOT NULL,
    tipo            VARCHAR(40)  NOT NULL,
    titulo          VARCHAR(120) NOT NULL,
    mensagem        VARCHAR(500) NOT NULL,
    agendamento_id  BIGINT       NULL,
    lida            TINYINT(1)   NOT NULL DEFAULT 0,
    criado_em       DATETIME     NOT NULL,
    INDEX idx_notificacao_usuario (usuario_id, criado_em),
    CONSTRAINT fk_notificacao_usuario FOREIGN KEY (usuario_id) REFERENCES usuario (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE lancamento (
    id               BIGINT AUTO_INCREMENT PRIMARY KEY,
    barbearia_id     BIGINT        NOT NULL,
    tipo             VARCHAR(10)   NOT NULL,
    categoria        VARCHAR(60)   NOT NULL,
    descricao        VARCHAR(255)  NULL,
    valor            DECIMAL(10,2) NOT NULL,
    data             DATE          NOT NULL,
    forma_pagamento  VARCHAR(20)   NULL,
    agendamento_id   BIGINT        NULL,
    barbeiro_id      BIGINT        NULL,
    criado_em        DATETIME      NOT NULL,
    INDEX idx_lancamento_barbearia (barbearia_id, data),
    CONSTRAINT uk_lancamento_agendamento UNIQUE (agendamento_id),
    CONSTRAINT fk_lancamento_barbearia FOREIGN KEY (barbearia_id) REFERENCES barbearia (id),
    CONSTRAINT fk_lancamento_agendamento FOREIGN KEY (agendamento_id) REFERENCES agendamento (id),
    CONSTRAINT fk_lancamento_barbeiro FOREIGN KEY (barbeiro_id) REFERENCES barbeiro (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE cobranca (
    id               BIGINT AUTO_INCREMENT PRIMARY KEY,
    barbearia_id     BIGINT        NOT NULL,
    competencia      CHAR(7)       NOT NULL,
    valor            DECIMAL(10,2) NOT NULL,
    vencimento       DATE          NOT NULL,
    status           VARCHAR(15)   NOT NULL,
    pago_em          DATE          NULL,
    forma_pagamento  VARCHAR(20)   NULL,
    observacao       VARCHAR(255)  NULL,
    criado_em        DATETIME      NOT NULL,
    CONSTRAINT uk_cobranca_competencia UNIQUE (barbearia_id, competencia),
    CONSTRAINT fk_cobranca_barbearia FOREIGN KEY (barbearia_id) REFERENCES barbearia (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

INSERT INTO tipo_cabelo (nome, descricao, ordem, ativo) VALUES
    ('Liso',     'Fios retos, sem curvatura', 1, 1),
    ('Ondulado', 'Fios em forma de S suave',  2, 1),
    ('Cacheado', 'Cachos definidos',          3, 1),
    ('Crespo',   'Fios em zigue-zague, alto volume', 4, 1);

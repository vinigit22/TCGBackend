-- =====================================================================
-- Perfil "h2" (TEMPORARIO, para testes)
-- Mesmos dados de teste do TorneioTCG_SQL.sql, com uma diferenca:
-- todas as contas abaixo tem a senha real 123456 (hash BCrypt),
-- entao da para fazer login com elas.
-- O administrador (admin@tcg.com / admin123) e criado pelo AdminInitializer.
-- =====================================================================

INSERT INTO jogo (nome, slug, ativo) VALUES
    ('Magic: The Gathering', 'magic',     TRUE),
    ('Pokemon TCG',          'pokemon',   TRUE),
    ('Yu-Gi-Oh!',            'yugioh',    TRUE),
    ('One Piece Card Game',  'one-piece', TRUE),
    ('Digimon Card Game',    'digimon',   TRUE);

INSERT INTO formato (jogo_id, nome, ativo) VALUES
    (1, 'Standard', TRUE), (1, 'Modern', TRUE), (1, 'Commander', TRUE), (1, 'Pauper', TRUE),
    (2, 'Standard', TRUE), (2, 'Expandido', TRUE),
    (3, 'Advanced', TRUE),
    (4, 'Standard', TRUE);

-- Senha de todas: 123456
INSERT INTO conta (email, senha_hash, tipo, ativo, email_verificado, criado_em, atualizado_em) VALUES
    ('contato@cardhouse.com.br', '$2a$10$rEQMXhxVZ2JrRhQSdYD/EOI2sEXZA36IuE1MVjJLslnUCHSyY4Gs6', 'LOJA',    TRUE, TRUE, LOCALTIMESTAMP, LOCALTIMESTAMP),
    ('eric@email.com',           '$2a$10$rEQMXhxVZ2JrRhQSdYD/EOI2sEXZA36IuE1MVjJLslnUCHSyY4Gs6', 'JOGADOR', TRUE, TRUE, LOCALTIMESTAMP, LOCALTIMESTAMP),
    ('samuel@email.com',         '$2a$10$rEQMXhxVZ2JrRhQSdYD/EOI2sEXZA36IuE1MVjJLslnUCHSyY4Gs6', 'JOGADOR', TRUE, TRUE, LOCALTIMESTAMP, LOCALTIMESTAMP),
    ('vinicius@email.com',       '$2a$10$rEQMXhxVZ2JrRhQSdYD/EOI2sEXZA36IuE1MVjJLslnUCHSyY4Gs6', 'JOGADOR', TRUE, TRUE, LOCALTIMESTAMP, LOCALTIMESTAMP),
    ('lucas@email.com',          '$2a$10$rEQMXhxVZ2JrRhQSdYD/EOI2sEXZA36IuE1MVjJLslnUCHSyY4Gs6', 'JOGADOR', TRUE, TRUE, LOCALTIMESTAMP, LOCALTIMESTAMP);

INSERT INTO endereco (cep, logradouro, numero, bairro, cidade, estado, latitude, longitude, criado_em) VALUES
    ('01310100', 'Avenida Paulista', '1000', 'Bela Vista', 'Sao Paulo', 'SP', -23.56320000, -46.65430000, LOCALTIMESTAMP);

INSERT INTO loja (conta_id, nome, slug, descricao, endereco_id, verificada, criado_em, atualizado_em) VALUES
    (1, 'Card House', 'card-house', 'Loja especializada em card games na regiao central de Sao Paulo.', 1, FALSE, LOCALTIMESTAMP, LOCALTIMESTAMP);

INSERT INTO jogador (conta_id, nome, nickname, cidade, estado, criado_em, atualizado_em) VALUES
    (2, 'Eric Abreu',       'ericabreu',   'Sao Paulo', 'SP', LOCALTIMESTAMP, LOCALTIMESTAMP),
    (3, 'Samuel Barbosa',   'sambarbosa',  'Sao Paulo', 'SP', LOCALTIMESTAMP, LOCALTIMESTAMP),
    (4, 'Vinicius Marques', 'vinimarques', 'Sao Paulo', 'SP', LOCALTIMESTAMP, LOCALTIMESTAMP),
    (5, 'Lucas Souza',      'lucassz',     'Guarulhos', 'SP', LOCALTIMESTAMP, LOCALTIMESTAMP);

INSERT INTO loja_membro (loja_id, conta_id, papel, ativo, criado_em) VALUES
    (1, 1, 'PROPRIETARIO', TRUE, LOCALTIMESTAMP);

-- Evento (nao e torneio: encontro de troca de cartas)
INSERT INTO evento (loja_id, titulo, descricao, tipo, vagas_max, data_inicio, status, criado_em, atualizado_em) VALUES
    (1, 'Dia da Troca', 'Encontro livre para troca de cartas entre colecionadores.',
     'TROCA', 40, '2026-09-12 14:00:00', 'PUBLICADO', LOCALTIMESTAMP, LOCALTIMESTAMP);

-- Torneio com 4 vagas e 4 confirmados, pronto para testar a chave completa
INSERT INTO torneio (loja_id, jogo_id, formato_id, titulo, descricao, vagas_max, taxa_inscricao, premiacao,
                     inscricoes_ate, data_inicio, status, criado_em, atualizado_em) VALUES
    (1, 1, 1, 'Standard Semanal', 'Torneio semanal de Magic, formato Standard, melhor de 3.',
     4, 25.00, 'R$ 200 em creditos + 3 boosters para o campeao',
     '2026-09-05 18:00:00', '2026-09-05 19:00:00', 'INSCRICOES_ABERTAS', LOCALTIMESTAMP, LOCALTIMESTAMP);

INSERT INTO inscricao (torneio_id, jogador_id, status, pagamento_status, inscrito_em, check_in_em) VALUES
    (1, 2, 'CONFIRMADO', 'PAGO', LOCALTIMESTAMP, LOCALTIMESTAMP),
    (1, 3, 'CONFIRMADO', 'PAGO', LOCALTIMESTAMP, LOCALTIMESTAMP),
    (1, 4, 'CONFIRMADO', 'PAGO', LOCALTIMESTAMP, LOCALTIMESTAMP),
    (1, 5, 'CONFIRMADO', 'PAGO', LOCALTIMESTAMP, LOCALTIMESTAMP);

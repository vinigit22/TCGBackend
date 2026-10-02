-- =====================================================================
-- Perfil "h2" (TEMPORARIO, para testes)
-- Roda depois que o Hibernate cria as tabelas a partir das entidades.
-- Recria no H2 as views do TorneioTCG_SQL.sql (as procedures nao sao mais usadas pela API).
-- =====================================================================

-- Pagina de trofeus do jogador
CREATE OR REPLACE VIEW vw_trofeus AS
SELECT
    j.conta_id                                            AS jogador_id,
    j.nickname,
    j.nome,
    SUM(CASE WHEN r.colocacao = 1 THEN 1 ELSE 0 END)      AS ouro,
    SUM(CASE WHEN r.colocacao = 2 THEN 1 ELSE 0 END)      AS prata,
    SUM(CASE WHEN r.colocacao = 3 THEN 1 ELSE 0 END)      AS bronze,
    COUNT(r.id)                                           AS torneios_disputados
FROM jogador j
LEFT JOIN torneio_resultado r ON r.jogador_id = j.conta_id
GROUP BY j.conta_id, j.nickname, j.nome;

-- Agenda unificada da loja (eventos + torneios).
-- No H2 os status de evento e torneio sao tipos ENUM diferentes, por isso o CAST.
CREATE OR REPLACE VIEW vw_agenda_loja AS
SELECT 'EVENTO' AS categoria, e.id, e.loja_id, e.titulo, e.imagem,
       e.data_inicio, CAST(e.status AS VARCHAR(30)) AS status, CAST(NULL AS VARCHAR(100)) AS jogo
  FROM evento e WHERE e.deletado_em IS NULL
UNION ALL
SELECT 'TORNEIO' AS categoria, t.id, t.loja_id, t.titulo, t.imagem,
       t.data_inicio, CAST(t.status AS VARCHAR(30)) AS status, g.nome AS jogo
  FROM torneio t
  JOIN jogo g ON g.id = t.jogo_id
 WHERE t.deletado_em IS NULL;

-- Chave do torneio pronta para exibicao
CREATE OR REPLACE VIEW vw_chaveamento AS
SELECT
    r.torneio_id,
    r.numero        AS rodada,
    r.nome          AS nome_rodada,
    p.id            AS partida_id,
    p.mesa,
    ja.nickname     AS jogador_a,
    jb.nickname     AS jogador_b,
    p.games_a,
    p.games_b,
    p.resultado,
    jv.nickname     AS vencedor,
    p.status
FROM rodada r
JOIN partida p          ON p.rodada_id = r.id
LEFT JOIN inscricao ia  ON ia.id = p.inscricao_a_id
LEFT JOIN jogador   ja  ON ja.conta_id = ia.jogador_id
LEFT JOIN inscricao ib  ON ib.id = p.inscricao_b_id
LEFT JOIN jogador   jb  ON jb.conta_id = ib.jogador_id
LEFT JOIN inscricao iv  ON iv.id = p.vencedor_id
LEFT JOIN jogador   jv  ON jv.conta_id = iv.jogador_id;

-- Vagas restantes por torneio
CREATE OR REPLACE VIEW vw_torneio_vagas AS
SELECT
    t.id AS torneio_id,
    t.titulo,
    t.vagas_max,
    COUNT(CASE WHEN i.status IN ('INSCRITO','CONFIRMADO') THEN 1 END) AS inscritos,
    t.vagas_max - COUNT(CASE WHEN i.status IN ('INSCRITO','CONFIRMADO') THEN 1 END) AS vagas_restantes,
    COUNT(CASE WHEN i.status = 'LISTA_ESPERA' THEN 1 END) AS lista_espera,
    COUNT(CASE WHEN i.pagamento_status = 'PENDENTE' THEN 1 END) AS pagamentos_pendentes
FROM torneio t
LEFT JOIN inscricao i ON i.torneio_id = t.id
GROUP BY t.id, t.titulo, t.vagas_max;

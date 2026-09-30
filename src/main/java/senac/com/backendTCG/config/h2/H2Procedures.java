package senac.com.backendTCG.config.h2;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class H2Procedures {

    private static final String SQLSTATE_REGRA = "45000";

    private H2Procedures() {
    }
    public static void gerarChaveamento(Connection conn, long torneioId) throws SQLException {
        int vagas;
        String status;

        try (PreparedStatement ps = conn.prepareStatement("SELECT vagas_max, status FROM torneio WHERE id = ?")) {
            ps.setLong(1, torneioId);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) {
                    throw erro("Torneio nao encontrado.");
                }
                vagas = rs.getInt(1);
                status = rs.getString(2);
            }
        }

        if (!"INSCRICOES_ENCERRADAS".equals(status)) {
            throw erro("Encerre as inscricoes antes de gerar a chave.");
        }

        List<Long> confirmados = listarIds(conn,
                "SELECT id FROM inscricao WHERE torneio_id = ? AND status = 'CONFIRMADO'", torneioId);

        // Regra do projeto: a chave so fecha com potencia de 2 completa
        if (confirmados.size() != vagas) {
            throw erro("Numero de confirmados diferente das vagas. Chave exige potencia de 2 completa.");
        }

        int totalRodadas = Integer.numberOfTrailingZeros(vagas); // log2 de uma potencia de 2

        // Sorteio: a posicao na lista embaralhada vira o seed (1, 2, 3...)
        Collections.shuffle(confirmados);
        executar(conn, "UPDATE inscricao SET seed = NULL WHERE torneio_id = ?", torneioId);
        for (int i = 0; i < confirmados.size(); i++) {
            executar(conn, "UPDATE inscricao SET seed = ? WHERE id = ?", i + 1, confirmados.get(i));
        }

        // Cria as rodadas e as partidas vazias. partidas[r][m] = id da mesa m+1 da rodada r+1
        long[][] partidas = new long[totalRodadas][];
        for (int r = 1; r <= totalRodadas; r++) {
            int qtdPartidas = vagas >> r;

            long rodadaId = inserir(conn,
                    "INSERT INTO rodada (torneio_id, numero, nome, status) VALUES (?, ?, ?, ?)",
                    torneioId, r, nomeDaRodada(qtdPartidas, r), r == 1 ? "EM_ANDAMENTO" : "AGUARDANDO");

            partidas[r - 1] = new long[qtdPartidas];
            for (int mesa = 1; mesa <= qtdPartidas; mesa++) {
                partidas[r - 1][mesa - 1] = inserir(conn,
                        "INSERT INTO partida (rodada_id, mesa, status, games_a, games_b, games_empate) "
                                + "VALUES (?, ?, ?, 0, 0, 0)",
                        rodadaId, mesa, r == 1 ? "PRONTA" : "AGUARDANDO");
            }
        }

        // Liga cada partida a sua sucessora: mesas 1 e 2 alimentam a mesa 1 da rodada seguinte (slots A e B)
        for (int r = 0; r < totalRodadas - 1; r++) {
            for (int m = 0; m < partidas[r].length; m++) {
                executar(conn, "UPDATE partida SET proxima_partida_id = ?, proximo_slot = ? WHERE id = ?",
                        partidas[r + 1][m / 2], m % 2 == 0 ? "A" : "B", partidas[r][m]);
            }
        }

        // Primeira rodada: seed 1 x seed 2, seed 3 x seed 4...
        for (int m = 0; m < partidas[0].length; m++) {
            executar(conn, "UPDATE partida SET inscricao_a_id = ?, inscricao_b_id = ? WHERE id = ?",
                    confirmados.get(2 * m), confirmados.get(2 * m + 1), partidas[0][m]);
        }

        executar(conn,
                "UPDATE torneio SET status = 'EM_ANDAMENTO', total_rodadas = ?, atualizado_em = LOCALTIMESTAMP "
                        + "WHERE id = ?",
                totalRodadas, torneioId);
    }

    // Equivalente a sp_registrar_resultado: grava o placar e avanca o vencedor na chave
    public static void registrarResultado(Connection conn, long partidaId, int gamesA, int gamesB,
                                          int gamesEmpate, String resultado) throws SQLException {
        Long inscricaoA;
        Long inscricaoB;
        Long proximaPartida;
        String proximoSlot;

        try (PreparedStatement ps = conn.prepareStatement(
                "SELECT inscricao_a_id, inscricao_b_id, proxima_partida_id, proximo_slot FROM partida WHERE id = ?")) {
            ps.setLong(1, partidaId);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) {
                    throw erro("Partida nao encontrada.");
                }
                inscricaoA = rs.getObject(1, Long.class);
                inscricaoB = rs.getObject(2, Long.class);
                proximaPartida = rs.getObject(3, Long.class);
                proximoSlot = rs.getString(4);
            }
        }

        Long vencedor = switch (resultado) {
            case "VITORIA_A", "WO_A" -> inscricaoA;
            case "VITORIA_B", "WO_B" -> inscricaoB;
            default -> null;
        };

        // Empate nao resolve a chave: exige game de desempate
        if ("EMPATE".equals(resultado)) {
            throw erro("Empate registrado: e necessario um game de desempate para definir quem avanca.");
        }

        executar(conn,
                "UPDATE partida SET games_a = ?, games_b = ?, games_empate = ?, resultado = ?, vencedor_id = ?, "
                        + "status = 'FINALIZADA', finalizada_em = LOCALTIMESTAMP WHERE id = ?",
                gamesA, gamesB, gamesEmpate, resultado, vencedor, partidaId);

        // Marca no-show na inscricao de quem perdeu por W.O.
        if ("WO_A".equals(resultado)) {
            executar(conn, "UPDATE inscricao SET status = 'NO_SHOW' WHERE id = ?", inscricaoB);
        } else if ("WO_B".equals(resultado)) {
            executar(conn, "UPDATE inscricao SET status = 'NO_SHOW' WHERE id = ?", inscricaoA);
        }

        // Avanca o vencedor para a proxima partida
        if (proximaPartida != null && vencedor != null) {
            String coluna = "A".equals(proximoSlot) ? "inscricao_a_id" : "inscricao_b_id";
            executar(conn, "UPDATE partida SET " + coluna + " = ? WHERE id = ?", vencedor, proximaPartida);
            executar(conn,
                    "UPDATE partida SET status = 'PRONTA' WHERE id = ? "
                            + "AND inscricao_a_id IS NOT NULL AND inscricao_b_id IS NOT NULL",
                    proximaPartida);
        }
    }

    private static String nomeDaRodada(int qtdPartidas, int numero) {
        return switch (qtdPartidas) {
            case 1 -> "Final";
            case 2 -> "Semifinal";
            case 4 -> "Quartas de final";
            case 8 -> "Oitavas de final";
            default -> "Rodada " + numero;
        };
    }

    private static SQLException erro(String mensagem) {
        return new SQLException(mensagem, SQLSTATE_REGRA);
    }

    private static List<Long> listarIds(Connection conn, String sql, Object... params) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            preencher(ps, params);
            List<Long> ids = new ArrayList<>();
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    ids.add(rs.getLong(1));
                }
            }
            return ids;
        }
    }

    private static void executar(Connection conn, String sql, Object... params) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            preencher(ps, params);
            ps.executeUpdate();
        }
    }

    private static long inserir(Connection conn, String sql, Object... params) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            preencher(ps, params);
            ps.executeUpdate();
            try (ResultSet rs = ps.getGeneratedKeys()) {
                rs.next();
                return rs.getLong(1);
            }
        }
    }

    private static void preencher(PreparedStatement ps, Object... params) throws SQLException {
        for (int i = 0; i < params.length; i++) {
            ps.setObject(i + 1, params[i]);
        }
    }
}

package senac.com.backendTCG.service;

import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import senac.com.backendTCG.dto.ResultadoPartidaRequest;
import senac.com.backendTCG.entity.Game;
import senac.com.backendTCG.entity.Inscricao;
import senac.com.backendTCG.entity.Partida;
import senac.com.backendTCG.entity.Rodada;
import senac.com.backendTCG.entity.Torneio;
import senac.com.backendTCG.entity.TorneioResultado;
import senac.com.backendTCG.entity.enums.ResultadoGame;
import senac.com.backendTCG.entity.enums.ResultadoPartida;
import senac.com.backendTCG.entity.enums.SlotPartida;
import senac.com.backendTCG.entity.enums.StatusInscricao;
import senac.com.backendTCG.entity.enums.StatusPartida;
import senac.com.backendTCG.entity.enums.StatusRodada;
import senac.com.backendTCG.entity.enums.StatusTorneio;
import senac.com.backendTCG.entity.enums.TipoNotificacao;
import senac.com.backendTCG.repository.GameRepository;
import senac.com.backendTCG.repository.InscricaoRepository;
import senac.com.backendTCG.repository.PartidaRepository;
import senac.com.backendTCG.repository.RodadaRepository;
import senac.com.backendTCG.repository.TorneioResultadoRepository;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

// Chave eliminatoria simples: sorteio com byes, avanco do vencedor, encerramento das rodadas,
// finalizacao do torneio e classificacao final.
// Substitui as procedures sp_gerar_chaveamento e sp_registrar_resultado do script SQL, que exigiam
// a chave completa (sem byes), paravam em 64 vagas e nao finalizavam o torneio. As triggers continuam valendo.
// Chamado pelos services de torneio, partida e game, que ja conferiram as permissoes.
@Service
@RequiredArgsConstructor
public class ChaveamentoService {

    private static final String OBSERVACAO_BYE = "Avançou sem adversário (bye)";
    private static final int MAXIMO_GAMES = 5;

    private final PartidaRepository partidaRepository;
    private final RodadaRepository rodadaRepository;
    private final InscricaoRepository inscricaoRepository;
    private final TorneioResultadoRepository torneioResultadoRepository;
    private final GameRepository gameRepository;
    private final NotificacaoService notificacaoService;
    private final EntityManager entityManager;

    // ---------------------------------------------------------------------
    // Geracao da chave
    // ---------------------------------------------------------------------

    // Sorteia os CONFIRMADOS numa chave do tamanho da menor potencia de 2 que comporta todos.
    // Ex.: 5 confirmados -> chave de 8, com 3 byes na primeira rodada.
    public void gerar(Torneio torneio) {
        if (torneio.getStatus() != StatusTorneio.INSCRICOES_ENCERRADAS) {
            throw conflito("Encerre as inscrições antes de gerar a chave.");
        }

        if (rodadaRepository.existsByTorneio_Id(torneio.getId())) {
            throw conflito("Este torneio já tem rodadas. A chave só pode ser gerada uma vez.");
        }

        List<Inscricao> confirmados = new ArrayList<>(inscricaoRepository.findByTorneio_IdAndStatusIn(
                torneio.getId(), List.of(StatusInscricao.CONFIRMADO)));

        if (confirmados.size() < 2) {
            throw conflito("São necessários pelo menos 2 jogadores confirmados (check-in) para gerar a chave.");
        }

        sortearSeeds(torneio, confirmados);

        int tamanho = Integer.highestOneBit(confirmados.size() - 1) << 1;
        int totalRodadas = Integer.numberOfTrailingZeros(tamanho);
        List<List<Partida>> chave = criarRodadasEPartidas(torneio, tamanho, totalRodadas);

        posicionarJogadores(chave.get(0), confirmados, tamanho - confirmados.size());

        torneio.setStatus(StatusTorneio.EM_ANDAMENTO);
        torneio.setTotalRodadas(totalRodadas);

        // So quem esta na chave (CONFIRMADO) e avisado: quem nao fez check-in ficou de fora
        for (Inscricao inscricao : confirmados) {
            notificacaoService.notificar(inscricao.getJogador().getConta(), TipoNotificacao.PAREAMENTO, "Chave sorteada",
                    "A chave do torneio '" + torneio.getTitulo() + "' foi sorteada. Confira seu adversário!",
                    torneio.getId(), null, null);
        }

        // Quem ficou sem adversario ja avanca
        for (Partida partida : chave.get(0)) {
            if (partida.getInscricaoB() == null) {
                finalizar(partida, ResultadoPartida.WO_A, partida.getInscricaoA(), OBSERVACAO_BYE);
            }
        }

        // A view vw_chaveamento e lida via JDBC logo em seguida: grava tudo antes
        entityManager.flush();
    }

    // A ordem embaralhada vira o seed (1, 2, 3...). Limpa os seeds antigos antes por causa do
    // UNIQUE (torneio_id, seed).
    private void sortearSeeds(Torneio torneio, List<Inscricao> confirmados) {
        inscricaoRepository.filtrar(torneio.getId(), null, null).forEach(i -> i.setSeed(null));
        entityManager.flush();

        Collections.shuffle(confirmados);
        for (int i = 0; i < confirmados.size(); i++) {
            confirmados.get(i).setSeed(i + 1);
        }
    }

    // chave.get(r).get(m) = mesa m+1 da rodada r+1. Mesas 1 e 2 alimentam a mesa 1 da rodada seguinte
    // (slots A e B), mesas 3 e 4 a mesa 2, e assim por diante.
    private List<List<Partida>> criarRodadasEPartidas(Torneio torneio, int tamanho, int totalRodadas) {
        List<List<Partida>> chave = new ArrayList<>();
        LocalDateTime agora = LocalDateTime.now();

        for (int numero = 1; numero <= totalRodadas; numero++) {
            int qtdPartidas = tamanho >> numero;

            Rodada rodada = new Rodada();
            rodada.setTorneio(torneio);
            rodada.setNumero(numero);
            rodada.setNome(nomeDaRodada(qtdPartidas, numero));
            rodada.setStatus(numero == 1 ? StatusRodada.EM_ANDAMENTO : StatusRodada.AGUARDANDO);
            rodada.setIniciadaEm(numero == 1 ? agora : null);
            rodadaRepository.save(rodada);

            List<Partida> partidas = new ArrayList<>();
            for (int mesa = 1; mesa <= qtdPartidas; mesa++) {
                Partida partida = new Partida();
                partida.setRodada(rodada);
                partida.setMesa(mesa);
                partidas.add(partidaRepository.save(partida));
            }
            chave.add(partidas);
        }

        for (int r = 0; r < totalRodadas - 1; r++) {
            for (int m = 0; m < chave.get(r).size(); m++) {
                Partida partida = chave.get(r).get(m);
                partida.setProximaPartidaId(chave.get(r + 1).get(m / 2).getId());
                partida.setProximoSlot(m % 2 == 0 ? SlotPartida.A : SlotPartida.B);
            }
        }

        return chave;
    }

    // Os byes vao primeiro para as mesas impares (1, 3, 5...) e depois para as pares,
    // para nao se concentrarem num lado da chave
    private void posicionarJogadores(List<Partida> primeiraRodada, List<Inscricao> sorteados, int byes) {
        List<Integer> mesasComBye = new ArrayList<>();
        for (int mesa = 1; mesa <= primeiraRodada.size(); mesa += 2) {
            mesasComBye.add(mesa);
        }
        for (int mesa = 2; mesa <= primeiraRodada.size(); mesa += 2) {
            mesasComBye.add(mesa);
        }
        mesasComBye = mesasComBye.subList(0, byes);

        Iterator<Inscricao> proximo = sorteados.iterator();
        for (Partida partida : primeiraRodada) {
            partida.setInscricaoA(proximo.next());
            if (!mesasComBye.contains(partida.getMesa())) {
                partida.setInscricaoB(proximo.next());
                partida.setStatus(StatusPartida.PRONTA);
            }
        }
    }

    // ---------------------------------------------------------------------
    // Resultados
    // ---------------------------------------------------------------------

    public void registrarResultado(Partida partida, ResultadoPartidaRequest request) {
        verificarPartidaEmJogo(partida);

        ResultadoPartida resultado = request.resultado();
        boolean temGames = atualizarPlacarPelosGames(partida);
        if (!temGames) {
            partida.setGamesA(valorOuZero(request.gamesA()));
            partida.setGamesB(valorOuZero(request.gamesB()));
            partida.setGamesEmpate(valorOuZero(request.gamesEmpate()));
        }
        validarPlacar(partida, resultado);

        // Sem esta regra o torneio terminaria sem campeao e com dois 2os lugares
        if (resultado == ResultadoPartida.DUPLO_NO_SHOW && partida.getProximaPartidaId() == null
                && ehFinalDaChave(partida.getRodada(), partida.getRodada().getTorneio())) {
            throw conflito("A final precisa de um vencedor. Registre W.O. (WO_A ou WO_B) para quem compareceu.");
        }

        if (resultado == ResultadoPartida.EMPATE) {
            // A chave precisa de um vencedor: a partida fica aguardando o desempate
            partida.setResultado(ResultadoPartida.EMPATE);
            partida.setVencedor(null);
            partida.setStatus(StatusPartida.EM_ANDAMENTO);
            if (partida.getIniciadaEm() == null) {
                partida.setIniciadaEm(LocalDateTime.now());
            }
            notificarJogadores(partida, TipoNotificacao.RESULTADO_REGISTRADO, "Partida empatada",
                    "Sua partida da " + partida.getRodada().getNome() + " (mesa " + partida.getMesa()
                            + ") empatou e vai para o desempate.");
            return;
        }

        Inscricao vencedor = switch (resultado) {
            case VITORIA_A, WO_A -> partida.getInscricaoA();
            case VITORIA_B, WO_B -> partida.getInscricaoB();
            case DUPLO_NO_SHOW, EMPATE -> null;
        };

        finalizar(partida, resultado, vencedor, null);
        notificarResultado(partida);
    }

    // Decide uma partida empatada. Se a partida usa games, o desempate vira o proximo game; se ela ja tem
    // os 5 games (ex.: 2 x 2 com um game empatado), o ponto do desempate entra direto no placar.
    public void registrarDesempate(Partida partida, SlotPartida vencedor) {
        verificarPartidaEmJogo(partida);

        if (partida.getResultado() != ResultadoPartida.EMPATE) {
            throw conflito("O desempate só vale para partidas empatadas. Registre o empate em POST /partidas/{id}/resultado.");
        }

        List<Game> games = gameRepository.findByPartida_IdOrderByNumeroAsc(partida.getId());
        int proximoNumero = games.isEmpty() ? 1 : games.get(games.size() - 1).getNumero() + 1;

        if (games.isEmpty() || proximoNumero > MAXIMO_GAMES) {
            if (vencedor == SlotPartida.A) {
                partida.setGamesA(partida.getGamesA() + 1);
            } else {
                partida.setGamesB(partida.getGamesB() + 1);
            }
        } else {
            Game desempate = new Game();
            desempate.setPartida(partida);
            desempate.setNumero(proximoNumero);
            desempate.setResultado(vencedor == SlotPartida.A ? ResultadoGame.A : ResultadoGame.B);
            gameRepository.save(desempate);
            atualizarPlacarPelosGames(partida);
        }

        boolean venceuA = vencedor == SlotPartida.A;
        finalizar(partida,
                venceuA ? ResultadoPartida.VITORIA_A : ResultadoPartida.VITORIA_B,
                venceuA ? partida.getInscricaoA() : partida.getInscricaoB(),
                "Decidida no desempate");
        notificarResultado(partida);
    }

    // Desfaz o resultado de uma partida finalizada para que ele seja corrigido: tira o vencedor da partida
    // seguinte, desfaz o NO_SHOW do W.O. e reabre a rodada. So vale enquanto a partida seguinte nao comecou;
    // a final nao chega aqui porque o torneio FINALIZADO ja nao pode ser alterado.
    public void reabrir(Partida partida) {
        if (partida.getStatus() != StatusPartida.FINALIZADA) {
            throw conflito("Só partidas finalizadas podem ser reabertas.");
        }
        if (partida.getInscricaoA() == null || partida.getInscricaoB() == null) {
            throw conflito("Esta partida foi decidida sem adversário (bye) e não tem resultado para corrigir.");
        }

        Rodada rodada = partida.getRodada();

        if (partida.getProximaPartidaId() != null) {
            Partida proxima = buscarProxima(partida);
            boolean proximaComecou = proxima.getStatus() == StatusPartida.EM_ANDAMENTO
                    || proxima.getStatus() == StatusPartida.FINALIZADA
                    || !gameRepository.findByPartida_IdOrderByNumeroAsc(proxima.getId()).isEmpty();
            if (proximaComecou) {
                throw conflito("A partida seguinte (mesa " + proxima.getMesa() + " da " + proxima.getRodada().getNome()
                        + ") já começou. Reabra aquela partida primeiro.");
            }

            if (partida.getProximoSlot() == SlotPartida.A) {
                proxima.setInscricaoA(null);
            } else {
                proxima.setInscricaoB(null);
            }
            proxima.setStatus(StatusPartida.AGUARDANDO);
        }

        ResultadoPartida anterior = partida.getResultado();
        if (anterior == ResultadoPartida.WO_A || anterior == ResultadoPartida.DUPLO_NO_SHOW) {
            desfazerNoShow(partida.getInscricaoB());
        }
        if (anterior == ResultadoPartida.WO_B || anterior == ResultadoPartida.DUPLO_NO_SHOW) {
            desfazerNoShow(partida.getInscricaoA());
        }

        partida.setResultado(null);
        partida.setVencedor(null);
        partida.setFinalizadaEm(null);
        partida.setStatus(StatusPartida.EM_ANDAMENTO);
        if (partida.getIniciadaEm() == null) {
            partida.setIniciadaEm(LocalDateTime.now());
        }

        // A rodada volta a ficar em andamento e a seguinte volta a aguardar
        if (rodada.getStatus() == StatusRodada.ENCERRADA) {
            rodada.setStatus(StatusRodada.EM_ANDAMENTO);
            rodada.setEncerradaEm(null);

            rodadaRepository.findByTorneio_IdAndNumero(rodada.getTorneio().getId(), rodada.getNumero() + 1)
                    .filter(seguinte -> seguinte.getStatus() == StatusRodada.EM_ANDAMENTO)
                    .ifPresent(seguinte -> {
                        seguinte.setStatus(StatusRodada.AGUARDANDO);
                        seguinte.setIniciadaEm(null);
                    });
        }

        notificarJogadores(partida, TipoNotificacao.RESULTADO_REGISTRADO, "Resultado em revisão",
                "O resultado da sua partida da " + rodada.getNome() + " (mesa " + partida.getMesa()
                        + ") foi reaberto pela loja para correção.");
    }

    // Recalcula o placar da partida a partir dos games cadastrados. Devolve false se nao houver games.
    public boolean atualizarPlacarPelosGames(Partida partida) {
        List<Game> games = gameRepository.findByPartida_IdOrderByNumeroAsc(partida.getId());
        if (games.isEmpty()) {
            return false;
        }

        partida.setGamesA(contar(games, ResultadoGame.A));
        partida.setGamesB(contar(games, ResultadoGame.B));
        partida.setGamesEmpate(contar(games, ResultadoGame.EMPATE));

        if (partida.getStatus() == StatusPartida.PRONTA) {
            partida.setStatus(StatusPartida.EM_ANDAMENTO);
            partida.setIniciadaEm(LocalDateTime.now());
        }
        return true;
    }

    public void verificarPartidaEmJogo(Partida partida) {
        if (partida.getStatus() == StatusPartida.FINALIZADA) {
            throw conflito("O resultado desta partida já foi registrado. Para corrigir, use POST /partidas/{id}/reabrir.");
        }
        if (partida.getInscricaoA() == null || partida.getInscricaoB() == null) {
            throw conflito("A partida ainda não tem os dois jogadores definidos.");
        }
    }

    // ---------------------------------------------------------------------
    // Avanco na chave
    // ---------------------------------------------------------------------

    private void finalizar(Partida partida, ResultadoPartida resultado, Inscricao vencedor, String observacao) {
        partida.setResultado(resultado);
        partida.setVencedor(vencedor);
        partida.setStatus(StatusPartida.FINALIZADA);
        partida.setFinalizadaEm(LocalDateTime.now());
        if (observacao != null) {
            partida.setObservacao(observacao);
        }

        // Quem nao compareceu fica como NO_SHOW (num bye nao ha adversario)
        if (resultado == ResultadoPartida.WO_A || resultado == ResultadoPartida.DUPLO_NO_SHOW) {
            marcarNoShow(partida.getInscricaoB());
        }
        if (resultado == ResultadoPartida.WO_B || resultado == ResultadoPartida.DUPLO_NO_SHOW) {
            marcarNoShow(partida.getInscricaoA());
        }

        Rodada rodada = partida.getRodada();
        Torneio torneio = rodada.getTorneio();

        if (partida.getProximaPartidaId() != null) {
            avancar(partida);
            encerrarRodadaSeConcluida(rodada);
        } else if (ehFinalDaChave(rodada, torneio)) {
            encerrarRodadaSeConcluida(rodada);
            finalizarTorneio(torneio);
        }
    }

    private void avancar(Partida partida) {
        Partida proxima = buscarProxima(partida);

        if (partida.getVencedor() != null) {
            if (partida.getProximoSlot() == SlotPartida.A) {
                proxima.setInscricaoA(partida.getVencedor());
            } else {
                proxima.setInscricaoB(partida.getVencedor());
            }
        }

        resolverProxima(proxima);
    }

    // A proxima partida so e decidida quando as duas que a alimentam terminaram. Se so chegou um
    // jogador (o outro lado teve duplo no-show), ele avanca direto.
    private void resolverProxima(Partida proxima) {
        boolean anterioresTerminadas = partidaRepository.findByProximaPartidaId(proxima.getId()).stream()
                .allMatch(p -> p.getStatus() == StatusPartida.FINALIZADA);
        if (!anterioresTerminadas) {
            return;
        }

        Inscricao a = proxima.getInscricaoA();
        Inscricao b = proxima.getInscricaoB();

        if (a != null && b != null) {
            proxima.setStatus(StatusPartida.PRONTA);
            notificarPartidaPronta(proxima);
        } else if (a != null) {
            finalizar(proxima, ResultadoPartida.WO_A, a, OBSERVACAO_BYE);
        } else if (b != null) {
            finalizar(proxima, ResultadoPartida.WO_B, b, OBSERVACAO_BYE);
        } else {
            finalizar(proxima, ResultadoPartida.DUPLO_NO_SHOW, null, "Nenhum jogador chegou a esta partida");
        }
    }

    // Encerra a rodada quando todas as partidas terminaram e abre a seguinte.
    // Uma rodada so encerra depois da anterior (com W.O. em cascata, a ordem poderia inverter).
    private void encerrarRodadaSeConcluida(Rodada rodada) {
        if (rodada.getStatus() == StatusRodada.ENCERRADA) {
            return;
        }

        Long torneioId = rodada.getTorneio().getId();
        boolean anteriorEncerrada = rodadaRepository.findByTorneio_IdAndNumero(torneioId, rodada.getNumero() - 1)
                .map(anterior -> anterior.getStatus() == StatusRodada.ENCERRADA)
                .orElse(true);
        boolean todasFinalizadas = partidaRepository.findByRodada_IdOrderByMesaAsc(rodada.getId()).stream()
                .allMatch(p -> p.getStatus() == StatusPartida.FINALIZADA);
        if (!anteriorEncerrada || !todasFinalizadas) {
            return;
        }

        LocalDateTime agora = LocalDateTime.now();
        rodada.setStatus(StatusRodada.ENCERRADA);
        rodada.setEncerradaEm(agora);

        rodadaRepository.findByTorneio_IdAndNumero(torneioId, rodada.getNumero() + 1).ifPresent(seguinte -> {
            seguinte.setStatus(StatusRodada.EM_ANDAMENTO);
            seguinte.setIniciadaEm(agora);
            encerrarRodadaSeConcluida(seguinte);
        });
    }

    // So a chave gerada automaticamente define totalRodadas; partidas montadas a mao nao finalizam o torneio
    private boolean ehFinalDaChave(Rodada rodada, Torneio torneio) {
        return torneio.getTotalRodadas() != null && rodada.getNumero().equals(torneio.getTotalRodadas());
    }

    private void finalizarTorneio(Torneio torneio) {
        // No MySQL, a trigger trg_partida_bloqueia_update recusa alterar partidas de torneio FINALIZADO:
        // grava as partidas antes de mudar o status do torneio
        entityManager.flush();

        registrarClassificacao(torneio);

        torneio.setStatus(StatusTorneio.FINALIZADO);
        torneio.setFinalizadoEm(LocalDateTime.now());

        notificacaoService.notificarInscritosTorneio(torneio, TipoNotificacao.TORNEIO_FINALIZADO, "Torneio finalizado",
                "O torneio '" + torneio.getTitulo() + "' foi finalizado. Confira a classificação.");
    }

    // 1o = campeao, 2o = vice. Quem perdeu numa rodada com N partidas fica em N + 1
    // (semifinal = 3o, quartas = 5o, oitavas = 9o). Vitorias por bye nao contam.
    // Jogadores que ja tinham resultado cadastrado a mao sao mantidos como estao.
    private void registrarClassificacao(Torneio torneio) {
        List<Partida> partidas = partidaRepository
                .findByRodada_Torneio_IdOrderByRodada_NumeroAscMesaAsc(torneio.getId());

        Map<Long, Long> partidasPorRodada = new LinkedHashMap<>();
        partidas.forEach(p -> partidasPorRodada.merge(p.getRodada().getId(), 1L, Long::sum));

        Map<Long, TorneioResultado> classificacao = new LinkedHashMap<>();

        for (Partida partida : partidas) {
            boolean disputada = partida.getInscricaoA() != null && partida.getInscricaoB() != null;

            for (Inscricao inscricao : jogadoresDa(partida)) {
                TorneioResultado linha = classificacao.computeIfAbsent(inscricao.getId(), id -> {
                    TorneioResultado novo = new TorneioResultado();
                    novo.setTorneio(torneio);
                    novo.setJogador(inscricao.getJogador());
                    return novo;
                });

                boolean venceu = partida.getVencedor() != null
                        && partida.getVencedor().getId().equals(inscricao.getId());

                if (venceu) {
                    if (disputada) {
                        linha.setVitorias(linha.getVitorias() + 1);
                    }
                    if (partida.getProximaPartidaId() == null) {
                        linha.setColocacao(1);
                    }
                } else {
                    linha.setDerrotas(linha.getDerrotas() + 1);
                    linha.setColocacao(partidasPorRodada.get(partida.getRodada().getId()).intValue() + 1);
                }
            }
        }

        classificacao.values().stream()
                .filter(linha -> linha.getColocacao() != null)
                .filter(linha -> !torneioResultadoRepository.existsByTorneio_IdAndJogador_ContaId(
                        torneio.getId(), linha.getJogador().getContaId()))
                .forEach(torneioResultadoRepository::save);
    }

    // ---------------------------------------------------------------------
    // Auxiliares
    // ---------------------------------------------------------------------

    private void validarPlacar(Partida partida, ResultadoPartida resultado) {
        int a = partida.getGamesA();
        int b = partida.getGamesB();

        boolean confere = switch (resultado) {
            case VITORIA_A -> a > b;
            case VITORIA_B -> b > a;
            case EMPATE -> a == b;
            case WO_A, WO_B, DUPLO_NO_SHOW -> true;
        };

        if (!confere) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "O placar " + a + " x " + b + " não confere com o resultado " + resultado + ".");
        }
    }

    private void marcarNoShow(Inscricao inscricao) {
        if (inscricao != null) {
            inscricao.setStatus(StatusInscricao.NO_SHOW);
        }
    }

    // Quem esta na chave fez check-in: ao desfazer o W.O. volta a CONFIRMADO
    private void desfazerNoShow(Inscricao inscricao) {
        if (inscricao != null && inscricao.getStatus() == StatusInscricao.NO_SHOW) {
            inscricao.setStatus(StatusInscricao.CONFIRMADO);
        }
    }

    private Partida buscarProxima(Partida partida) {
        return partidaRepository.findById(partida.getProximaPartidaId())
                .orElseThrow(() -> conflito("A partida seguinte desta chave (id " + partida.getProximaPartidaId()
                        + ") não existe mais. Ajuste a ligação da partida " + partida.getId() + " em PUT /partidas/{id}."));
    }

    private List<Inscricao> jogadoresDa(Partida partida) {
        List<Inscricao> jogadores = new ArrayList<>();
        if (partida.getInscricaoA() != null) {
            jogadores.add(partida.getInscricaoA());
        }
        if (partida.getInscricaoB() != null) {
            jogadores.add(partida.getInscricaoB());
        }
        return jogadores;
    }

    private void notificarResultado(Partida partida) {
        notificarJogadores(partida, TipoNotificacao.RESULTADO_REGISTRADO, "Resultado registrado",
                "O resultado da sua partida da " + partida.getRodada().getNome() + " (mesa " + partida.getMesa()
                        + ") foi registrado: " + partida.getResultado() + ".");
    }

    private void notificarPartidaPronta(Partida partida) {
        notificarJogadores(partida, TipoNotificacao.PAREAMENTO, "Partida pronta",
                "Sua partida da " + partida.getRodada().getNome() + " (mesa " + partida.getMesa() + ") do torneio '"
                        + partida.getRodada().getTorneio().getTitulo() + "' já tem os dois jogadores definidos.");
    }

    private void notificarJogadores(Partida partida, TipoNotificacao tipo, String titulo, String mensagem) {
        for (Inscricao inscricao : jogadoresDa(partida)) {
            notificacaoService.notificar(inscricao.getJogador().getConta(), tipo, titulo, mensagem,
                    partida.getRodada().getTorneio().getId(), null, partida.getId());
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

    private static int contar(List<Game> games, ResultadoGame resultado) {
        return (int) games.stream().filter(g -> g.getResultado() == resultado).count();
    }

    private static int valorOuZero(Integer valor) {
        return valor == null ? 0 : valor;
    }

    private static ResponseStatusException conflito(String mensagem) {
        return new ResponseStatusException(HttpStatus.CONFLICT, mensagem);
    }
}

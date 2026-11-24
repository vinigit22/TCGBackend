package senac.com.backendTCG.service;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import senac.com.backendTCG.dto.DesempateRequest;
import senac.com.backendTCG.dto.PartidaRequest;
import senac.com.backendTCG.dto.ResultadoPartidaRequest;
import senac.com.backendTCG.entity.Inscricao;
import senac.com.backendTCG.entity.Partida;
import senac.com.backendTCG.entity.Rodada;
import senac.com.backendTCG.entity.Torneio;
import senac.com.backendTCG.entity.enums.ResultadoPartida;
import senac.com.backendTCG.entity.enums.StatusPartida;
import senac.com.backendTCG.entity.enums.TipoNotificacao;
import senac.com.backendTCG.repository.PartidaRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class PartidaService {

    private final PartidaRepository partidaRepository;
    private final RodadaService rodadaService;
    private final InscricaoService inscricaoService;
    private final TorneioService torneioService;
    private final ChaveamentoService chaveamentoService;
    private final NotificacaoService notificacaoService;
    private final PermissaoService permissaoService;

    @Value("${app.check-in.segundos:300}")
    private long segundosCheckIn;

    // Partidas de torneio excluido (soft delete) ficam de fora
    public List<Partida> listar(Long rodadaId, Long torneioId) {
        List<Partida> partidas;
        if (rodadaId != null) {
            partidas = partidaRepository.findByRodada_IdOrderByMesaAsc(rodadaId);
        } else if (torneioId != null) {
            partidas = partidaRepository.findByRodada_Torneio_IdOrderByRodada_NumeroAscMesaAsc(torneioId);
        } else {
            partidas = partidaRepository.findAll();
        }

        return partidas.stream()
                .filter(partida -> partida.getRodada().getTorneio().getDeletadoEm() == null)
                .toList();
    }

    public Partida buscarPorId(Long id) {
        return partidaRepository.findById(id)
                .filter(partida -> partida.getRodada().getTorneio().getDeletadoEm() == null)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Partida não encontrada"));
    }

    @Transactional
    public Partida criar(PartidaRequest request) {
        if (request.rodadaId() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Informe o rodadaId");
        }

        Rodada rodada = rodadaService.buscarPorId(request.rodadaId());
        torneioService.verificarPodeGerenciar(rodada.getTorneio());
        torneioService.verificarChaveManual(rodada.getTorneio());

        if (partidaRepository.existsByRodada_IdAndMesa(rodada.getId(), request.mesa())) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT, "Já existe uma partida na mesa " + request.mesa() + " desta rodada");
        }

        Partida partida = new Partida();
        partida.setRodada(rodada);
        preencher(partida, request);

        return partidaRepository.save(partida);
    }

    // Na chave gerada automaticamente o PUT so muda mesa e observacao: jogadores, ligacoes, status e
    // resultado mudam por /resultado, /desempate e /reabrir, que mantem a chave e a classificacao coerentes.
    // Campos nao informados (null) contam como "mantidos".
    @Transactional
    public Partida atualizar(Long id, PartidaRequest request) {
        Partida partida = buscarPorId(id);
        Torneio torneio = partida.getRodada().getTorneio();
        torneioService.verificarPodeGerenciar(torneio);

        if (partidaRepository.existsByRodada_IdAndMesaAndIdNot(partida.getRodada().getId(), request.mesa(), id)) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT, "Já existe uma partida na mesa " + request.mesa() + " desta rodada");
        }

        if (torneioService.temChaveAutomatica(torneio)) {
            if (mudaEstrutura(partida, request)) {
                torneioService.verificarChaveManual(torneio);
            }
            partida.setMesa(request.mesa());
            partida.setObservacao(request.observacao());
        } else {
            preencher(partida, request);
        }

        return partidaRepository.save(partida);
    }

    // Registra o placar e o vencedor, marca NO_SHOW em caso de W.O. e avanca o vencedor na chave.
    // Empate deixa a partida aguardando o desempate. A final encerra o torneio e gera a classificacao.
    @Transactional
    public Partida registrarResultado(Long id, ResultadoPartidaRequest request) {
        Partida partida = buscarPorId(id);
        torneioService.verificarPodeGerenciar(partida.getRodada().getTorneio());

        chaveamentoService.registrarResultado(partida, request);
        return partida;
    }

    @Transactional
    public Partida registrarDesempate(Long id, DesempateRequest request) {
        Partida partida = buscarPorId(id);
        torneioService.verificarPodeGerenciar(partida.getRodada().getTorneio());

        chaveamentoService.registrarDesempate(partida, request.vencedor());
        return partida;
    }

    // Desfaz o resultado de uma partida finalizada para corrigi-lo (enquanto a partida seguinte nao comecou)
    @Transactional
    public Partida reabrir(Long id) {
        Partida partida = buscarPorId(id);
        torneioService.verificarPodeGerenciar(partida.getRodada().getTorneio());

        chaveamentoService.reabrir(partida);
        return partida;
    }

    // Loja convoca os jogadores: abre janela de 5 min para check-in e notifica os dois. A partida so comeca
    // (EM_ANDAMENTO) quando os dois confirmam presenca no app. Se o prazo acaba antes disso, o agendador
    // (TorneioScheduler) da W.O. a quem confirmou; se ninguem confirmou, ninguem avanca.
    @Transactional
    public Partida convocar(Long id) {
        Partida partida = buscarPorId(id);
        torneioService.verificarPodeGerenciar(partida.getRodada().getTorneio());

        if (partida.getInscricaoA() == null || partida.getInscricaoB() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "A partida precisa ter dois jogadores para ser convocada");
        }
        if (partida.getStatus() == StatusPartida.FINALIZADA) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Esta partida já foi encerrada");
        }
        if (partida.getStatus() == StatusPartida.EM_ANDAMENTO) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Esta partida já começou: os dois jogadores já confirmaram presença");
        }

        LocalDateTime agora = LocalDateTime.now();
        if (partida.getCheckInExpiraEm() != null && agora.isBefore(partida.getCheckInExpiraEm())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Os jogadores já foram chamados. Aguarde os 5 minutos para a confirmação de presença.");
        }

        partida.setCheckInExpiraEm(agora.plusSeconds(segundosCheckIn));
        partida.setCheckInAEm(null);
        partida.setCheckInBEm(null);
        Partida salva = partidaRepository.save(partida);

        Long torneioId = partida.getRodada().getTorneio().getId();
        String titulo = "Você foi chamado — Mesa " + partida.getMesa();
        String mensagem = "Confirme sua presença em até 5 minutos. Quem não confirmar perde a partida por W.O.";

        notificacaoService.notificar(partida.getInscricaoA().getJogador().getConta(),
                TipoNotificacao.CHECK_IN_SOLICITADO, titulo, mensagem, torneioId, null, partida.getId());
        notificacaoService.notificar(partida.getInscricaoB().getJogador().getConta(),
                TipoNotificacao.CHECK_IN_SOLICITADO, titulo, mensagem, torneioId, null, partida.getId());

        return salva;
    }

    // Jogador faz check-in na partida (dentro da janela de 5 minutos). Quando os dois confirmaram,
    // a partida comeca: vira EM_ANDAMENTO e a loja passa a poder lancar o resultado.
    @Transactional
    public Partida checkIn(Long id) {
        Partida partida = buscarPorId(id);

        if (partida.getStatus() == StatusPartida.FINALIZADA) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Esta partida já foi encerrada");
        }
        if (partida.getCheckInExpiraEm() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Esta partida ainda não foi convocada");
        }
        LocalDateTime agora = LocalDateTime.now();
        if (agora.isAfter(partida.getCheckInExpiraEm())) {
            throw new ResponseStatusException(HttpStatus.GONE, "O prazo de check-in expirou");
        }

        Long jogadorId = permissaoService.contaLogada().getId();

        if (partida.getInscricaoA() != null && partida.getInscricaoA().getJogador().getConta().getId().equals(jogadorId)) {
            if (partida.getCheckInAEm() == null) {
                partida.setCheckInAEm(agora);
            }
        } else if (partida.getInscricaoB() != null && partida.getInscricaoB().getJogador().getConta().getId().equals(jogadorId)) {
            if (partida.getCheckInBEm() == null) {
                partida.setCheckInBEm(agora);
            }
        } else {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Você não é participante desta partida");
        }

        if (partida.getCheckInAEm() != null && partida.getCheckInBEm() != null
                && partida.getStatus() == StatusPartida.PRONTA) {
            partida.setStatus(StatusPartida.EM_ANDAMENTO);
            partida.setIniciadaEm(agora);
        }

        return partidaRepository.save(partida);
    }

    // Partida em que o jogador logado foi chamado e ainda nao confirmou presenca (o app abre a tela de check-in)
    public Optional<Partida> convocacaoPendente() {
        Long contaId = permissaoService.contaLogada().getId();
        return partidaRepository.findConvocadasSemCheckIn(contaId, LocalDateTime.now()).stream()
                .filter(partida -> partida.getRodada().getTorneio().getDeletadoEm() == null)
                .findFirst();
    }

    // Os games da partida sao apagados junto (ON DELETE CASCADE). As partidas que apontavam para esta
    // (proxima partida) perdem a ligacao, em vez de ficar apontando para o vazio.
    @Transactional
    public void deletar(Long id) {
        Partida partida = buscarPorId(id);
        torneioService.verificarPodeGerenciar(partida.getRodada().getTorneio());
        torneioService.verificarChaveManual(partida.getRodada().getTorneio());

        partidaRepository.findByProximaPartidaId(partida.getId()).forEach(anterior -> {
            anterior.setProximaPartidaId(null);
            anterior.setProximoSlot(null);
        });
        partidaRepository.delete(partida);
    }

    private void preencher(Partida partida, PartidaRequest request) {
        Torneio torneio = partida.getRodada().getTorneio();

        Inscricao inscricaoA = buscarInscricaoDoTorneio(request.inscricaoAId(), torneio);
        Inscricao inscricaoB = buscarInscricaoDoTorneio(request.inscricaoBId(), torneio);

        // Mesma regra do CHECK ck_partida_oponentes
        if (inscricaoA != null && inscricaoB != null && inscricaoA.getId().equals(inscricaoB.getId())) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST, "Os dois lados da partida não podem ser a mesma inscrição");
        }

        if (request.proximaPartidaId() != null) {
            Partida proxima = buscarPorId(request.proximaPartidaId());

            if (!proxima.getRodada().getTorneio().getId().equals(torneio.getId())
                    || proxima.getId().equals(partida.getId())) {
                throw new ResponseStatusException(
                        HttpStatus.BAD_REQUEST, "A próxima partida precisa ser outra partida do mesmo torneio");
            }
        }

        partida.setMesa(request.mesa());
        partida.setInscricaoA(inscricaoA);
        partida.setInscricaoB(inscricaoB);
        partida.setProximaPartidaId(request.proximaPartidaId());
        partida.setProximoSlot(request.proximoSlot());
        if (request.status() != null) {
            partida.setStatus(request.status());
        }
        partida.setResultado(request.resultado());
        partida.setVencedor(calcularVencedor(request.resultado(), inscricaoA, inscricaoB));
        partida.setGamesA(request.gamesA() == null ? 0 : request.gamesA());
        partida.setGamesB(request.gamesB() == null ? 0 : request.gamesB());
        partida.setGamesEmpate(request.gamesEmpate() == null ? 0 : request.gamesEmpate());
        partida.setObservacao(request.observacao());

        if (partida.getStatus() == StatusPartida.EM_ANDAMENTO && partida.getIniciadaEm() == null) {
            partida.setIniciadaEm(LocalDateTime.now());
        }
        if (partida.getStatus() == StatusPartida.FINALIZADA && partida.getFinalizadaEm() == null) {
            partida.setFinalizadaEm(LocalDateTime.now());
        }
    }

    private boolean mudaEstrutura(Partida partida, PartidaRequest request) {
        return mudou(idDe(partida.getInscricaoA()), request.inscricaoAId())
                || mudou(idDe(partida.getInscricaoB()), request.inscricaoBId())
                || mudou(partida.getProximaPartidaId(), request.proximaPartidaId())
                || mudou(partida.getProximoSlot(), request.proximoSlot())
                || mudou(partida.getStatus(), request.status())
                || mudou(partida.getResultado(), request.resultado())
                || mudou(partida.getGamesA(), request.gamesA())
                || mudou(partida.getGamesB(), request.gamesB())
                || mudou(partida.getGamesEmpate(), request.gamesEmpate());
    }

    private static boolean mudou(Object atual, Object informado) {
        return informado != null && !informado.equals(atual);
    }

    private static Long idDe(Inscricao inscricao) {
        return inscricao == null ? null : inscricao.getId();
    }

    private Inscricao buscarInscricaoDoTorneio(Long inscricaoId, Torneio torneio) {
        if (inscricaoId == null) {
            return null;
        }

        Inscricao inscricao = inscricaoService.buscarPorId(inscricaoId);

        if (!inscricao.getTorneio().getId().equals(torneio.getId())) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST, "A inscrição " + inscricaoId + " não pertence a este torneio");
        }

        return inscricao;
    }

    // Mesma regra do CHECK ck_partida_resultado: vitoria/W.O. tem vencedor; empate e duplo no-show nao
    private Inscricao calcularVencedor(ResultadoPartida resultado, Inscricao inscricaoA, Inscricao inscricaoB) {
        if (resultado == null) {
            return null;
        }

        Inscricao vencedor = switch (resultado) {
            case VITORIA_A, WO_A -> inscricaoA;
            case VITORIA_B, WO_B -> inscricaoB;
            case EMPATE, DUPLO_NO_SHOW -> null;
        };

        if (vencedor == null && resultado != ResultadoPartida.EMPATE && resultado != ResultadoPartida.DUPLO_NO_SHOW) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST, "Defina os dois jogadores antes de informar o vencedor");
        }

        return vencedor;
    }
}

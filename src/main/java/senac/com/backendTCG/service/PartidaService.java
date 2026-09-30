package senac.com.backendTCG.service;

import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
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

@Service
@RequiredArgsConstructor
public class PartidaService {

    private final PartidaRepository partidaRepository;
    private final RodadaService rodadaService;
    private final InscricaoService inscricaoService;
    private final TorneioService torneioService;
    private final NotificacaoService notificacaoService;
    private final JdbcTemplate jdbcTemplate;
    private final EntityManager entityManager;

    public List<Partida> listar(Long rodadaId, Long torneioId) {
        if (rodadaId != null) {
            return partidaRepository.findByRodada_IdOrderByMesaAsc(rodadaId);
        }

        if (torneioId != null) {
            return partidaRepository.findByRodada_Torneio_IdOrderByRodada_NumeroAscMesaAsc(torneioId);
        }

        return partidaRepository.findAll();
    }

    public Partida buscarPorId(Long id) {
        return partidaRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Partida não encontrada"));
    }

    @Transactional
    public Partida criar(PartidaRequest request) {
        if (request.rodadaId() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Informe o rodadaId");
        }

        Rodada rodada = rodadaService.buscarPorId(request.rodadaId());
        torneioService.verificarPodeGerenciar(rodada.getTorneio());

        if (partidaRepository.existsByRodada_IdAndMesa(rodada.getId(), request.mesa())) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT, "Já existe uma partida na mesa " + request.mesa() + " desta rodada");
        }

        Partida partida = new Partida();
        partida.setRodada(rodada);
        preencher(partida, request);

        return partidaRepository.save(partida);
    }

    @Transactional
    public Partida atualizar(Long id, PartidaRequest request) {
        Partida partida = buscarPorId(id);
        torneioService.verificarPodeGerenciar(partida.getRodada().getTorneio());

        if (partidaRepository.existsByRodada_IdAndMesaAndIdNot(partida.getRodada().getId(), request.mesa(), id)) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT, "Já existe uma partida na mesa " + request.mesa() + " desta rodada");
        }

        preencher(partida, request);
        return partidaRepository.save(partida);
    }

    // Chama a procedure sp_registrar_resultado: grava o placar, define o vencedor,
    // marca NO_SHOW em caso de W.O. e avanca o vencedor para a proxima partida da chave
    @Transactional
    public Partida registrarResultado(Long id, ResultadoPartidaRequest request) {
        Partida partida = buscarPorId(id);
        torneioService.verificarPodeGerenciar(partida.getRodada().getTorneio());

        if (partida.getInscricaoA() == null || partida.getInscricaoB() == null) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT, "A partida ainda não tem os dois jogadores definidos");
        }

        jdbcTemplate.update("CALL sp_registrar_resultado(?, ?, ?, ?, ?)",
                id,
                request.gamesA(),
                request.gamesB(),
                request.gamesEmpate() == null ? 0 : request.gamesEmpate(),
                request.resultado().name());

        // A procedure alterou o banco por fora do JPA: recarrega o que sera devolvido
        entityManager.refresh(partida.getInscricaoA());
        entityManager.refresh(partida.getInscricaoB());
        entityManager.refresh(partida);

        String mensagem = "O resultado da sua partida na mesa " + partida.getMesa()
                + " (" + partida.getRodada().getNome() + ") foi registrado: " + partida.getResultado() + ".";

        for (Inscricao inscricao : List.of(partida.getInscricaoA(), partida.getInscricaoB())) {
            notificacaoService.notificar(inscricao.getJogador().getConta(), TipoNotificacao.RESULTADO_REGISTRADO,
                    "Resultado registrado", mensagem,
                    partida.getRodada().getTorneio().getId(), null, partida.getId());
        }

        return partida;
    }

    // Games e notificacoes da partida sao apagados junto (ON DELETE CASCADE)
    @Transactional
    public void deletar(Long id) {
        Partida partida = buscarPorId(id);
        torneioService.verificarPodeGerenciar(partida.getRodada().getTorneio());
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

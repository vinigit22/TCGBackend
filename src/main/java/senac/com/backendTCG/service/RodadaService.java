package senac.com.backendTCG.service;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import senac.com.backendTCG.dto.RodadaRequest;
import senac.com.backendTCG.entity.Partida;
import senac.com.backendTCG.entity.Rodada;
import senac.com.backendTCG.entity.Torneio;
import senac.com.backendTCG.entity.enums.StatusRodada;
import senac.com.backendTCG.entity.enums.TipoNotificacao;
import senac.com.backendTCG.repository.PartidaRepository;
import senac.com.backendTCG.repository.RodadaRepository;

import java.time.LocalDateTime;
import java.util.List;

// As rodadas normalmente sao criadas ao gerar a chave (ChaveamentoService);
// este CRUD serve para torneios montados a mao pela equipe da loja
@Service
@RequiredArgsConstructor
public class RodadaService {

    private final RodadaRepository rodadaRepository;
    private final PartidaRepository partidaRepository;
    private final TorneioService torneioService;
    private final NotificacaoService notificacaoService;

    // Rodadas de torneio excluido (soft delete) ficam de fora
    public List<Rodada> listar(Long torneioId) {
        List<Rodada> rodadas = torneioId == null
                ? rodadaRepository.findAll(Sort.by("torneio.id", "numero"))
                : rodadaRepository.findByTorneio_IdOrderByNumeroAsc(torneioId);

        return rodadas.stream()
                .filter(rodada -> rodada.getTorneio().getDeletadoEm() == null)
                .toList();
    }

    public Rodada buscarPorId(Long id) {
        return rodadaRepository.findById(id)
                .filter(rodada -> rodada.getTorneio().getDeletadoEm() == null)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Rodada não encontrada"));
    }

    @Transactional
    public Rodada criar(RodadaRequest request) {
        if (request.torneioId() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Informe o torneioId");
        }

        Torneio torneio = torneioService.buscarPorId(request.torneioId());
        torneioService.verificarPodeGerenciar(torneio);
        torneioService.verificarChaveManual(torneio);

        if (rodadaRepository.existsByTorneio_IdAndNumero(torneio.getId(), request.numero())) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT, "Já existe a rodada " + request.numero() + " neste torneio");
        }

        Rodada rodada = new Rodada();
        rodada.setTorneio(torneio);
        rodada.setNumero(request.numero());
        rodada.setNome(request.nome());
        aplicarStatus(rodada, request.status() == null ? StatusRodada.AGUARDANDO : request.status());

        return rodadaRepository.save(rodada);
    }

    // Na chave gerada automaticamente so o nome pode mudar: numero e status sao controlados pela chave
    @Transactional
    public Rodada atualizar(Long id, RodadaRequest request) {
        Rodada rodada = buscarPorId(id);
        Torneio torneio = rodada.getTorneio();
        torneioService.verificarPodeGerenciar(torneio);

        boolean mudaNumero = !request.numero().equals(rodada.getNumero());
        boolean mudaStatus = request.status() != null && request.status() != rodada.getStatus();
        if (mudaNumero || mudaStatus) {
            torneioService.verificarChaveManual(torneio);
        }

        if (rodadaRepository.existsByTorneio_IdAndNumeroAndIdNot(torneio.getId(), request.numero(), id)) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT, "Já existe a rodada " + request.numero() + " neste torneio");
        }

        rodada.setNumero(request.numero());
        rodada.setNome(request.nome());
        if (mudaStatus) {
            aplicarStatus(rodada, request.status());
        }

        return rodadaRepository.save(rodada);
    }

    // As partidas da rodada sao apagadas junto (ON DELETE CASCADE). As partidas de outras rodadas que
    // apontavam para elas (proxima partida) perdem a ligacao, em vez de ficar apontando para o vazio.
    @Transactional
    public void deletar(Long id) {
        Rodada rodada = buscarPorId(id);
        torneioService.verificarPodeGerenciar(rodada.getTorneio());
        torneioService.verificarChaveManual(rodada.getTorneio());

        for (Partida partida : partidaRepository.findByRodada_IdOrderByMesaAsc(rodada.getId())) {
            partidaRepository.findByProximaPartidaId(partida.getId()).forEach(anterior -> {
                anterior.setProximaPartidaId(null);
                anterior.setProximoSlot(null);
            });
        }

        rodadaRepository.delete(rodada);
    }

    private void aplicarStatus(Rodada rodada, StatusRodada status) {
        rodada.setStatus(status);

        if (status == StatusRodada.EM_ANDAMENTO) {
            if (rodada.getIniciadaEm() == null) {
                rodada.setIniciadaEm(LocalDateTime.now());
            }

            notificacaoService.notificarInscritosTorneio(rodada.getTorneio(), TipoNotificacao.RODADA_INICIADA,
                    "Rodada iniciada",
                    "Começou a " + rodada.getNome() + " do torneio '" + rodada.getTorneio().getTitulo() + "'.");
        }

        if (status == StatusRodada.ENCERRADA && rodada.getEncerradaEm() == null) {
            rodada.setEncerradaEm(LocalDateTime.now());
        }
    }
}

package senac.com.backendTCG.service;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import senac.com.backendTCG.dto.RodadaRequest;
import senac.com.backendTCG.entity.Rodada;
import senac.com.backendTCG.entity.Torneio;
import senac.com.backendTCG.entity.enums.StatusRodada;
import senac.com.backendTCG.entity.enums.TipoNotificacao;
import senac.com.backendTCG.repository.RodadaRepository;

import java.time.LocalDateTime;
import java.util.List;

// As rodadas normalmente sao criadas pela procedure sp_gerar_chaveamento;
// este CRUD serve para ajustes manuais da equipe da loja
@Service
@RequiredArgsConstructor
public class RodadaService {

    private final RodadaRepository rodadaRepository;
    private final TorneioService torneioService;
    private final NotificacaoService notificacaoService;

    public List<Rodada> listar(Long torneioId) {
        return torneioId == null
                ? rodadaRepository.findAll(Sort.by("torneio.id", "numero"))
                : rodadaRepository.findByTorneio_IdOrderByNumeroAsc(torneioId);
    }

    public Rodada buscarPorId(Long id) {
        return rodadaRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Rodada não encontrada"));
    }

    @Transactional
    public Rodada criar(RodadaRequest request) {
        if (request.torneioId() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Informe o torneioId");
        }

        Torneio torneio = torneioService.buscarPorId(request.torneioId());
        torneioService.verificarPodeGerenciar(torneio);

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

    @Transactional
    public Rodada atualizar(Long id, RodadaRequest request) {
        Rodada rodada = buscarPorId(id);
        torneioService.verificarPodeGerenciar(rodada.getTorneio());

        if (rodadaRepository.existsByTorneio_IdAndNumeroAndIdNot(rodada.getTorneio().getId(), request.numero(), id)) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT, "Já existe a rodada " + request.numero() + " neste torneio");
        }

        rodada.setNumero(request.numero());
        rodada.setNome(request.nome());
        if (request.status() != null && request.status() != rodada.getStatus()) {
            aplicarStatus(rodada, request.status());
        }

        return rodadaRepository.save(rodada);
    }

    // As partidas da rodada sao apagadas junto (ON DELETE CASCADE)
    @Transactional
    public void deletar(Long id) {
        Rodada rodada = buscarPorId(id);
        torneioService.verificarPodeGerenciar(rodada.getTorneio());
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

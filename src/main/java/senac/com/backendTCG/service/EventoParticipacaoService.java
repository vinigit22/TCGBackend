package senac.com.backendTCG.service;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import senac.com.backendTCG.dto.EventoParticipacaoRequest;
import senac.com.backendTCG.entity.Evento;
import senac.com.backendTCG.entity.EventoParticipacao;
import senac.com.backendTCG.entity.UsuarioJogador;
import senac.com.backendTCG.entity.enums.StatusEvento;
import senac.com.backendTCG.entity.enums.StatusParticipacao;
import senac.com.backendTCG.repository.EventoParticipacaoRepository;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class EventoParticipacaoService {

    private final EventoParticipacaoRepository eventoParticipacaoRepository;
    private final EventoService eventoService;
    private final UsuarioJogadorService usuarioJogadorService;
    private final PermissaoService permissaoService;

    public List<EventoParticipacao> listar(Long eventoId, Long jogadorId) {
        return eventoParticipacaoRepository.filtrar(eventoId, jogadorId);
    }

    public EventoParticipacao buscarPorId(Long id) {
        return eventoParticipacaoRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Participação não encontrada"));
    }

    @Transactional
    public EventoParticipacao participar(EventoParticipacaoRequest request) {
        if (request.eventoId() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Informe o eventoId");
        }

        Evento evento = eventoService.buscarPorId(request.eventoId());
        UsuarioJogador jogador =
                usuarioJogadorService.resolverInscrito(request.jogadorId(), evento.getLoja().getContaId());

        if (evento.getStatus() != StatusEvento.PUBLICADO && evento.getStatus() != StatusEvento.EM_ANDAMENTO) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Este evento não está aberto para participação");
        }

        // Se ja existe participacao cancelada, ela e reativada (uk_evento_participacao)
        EventoParticipacao participacao = eventoParticipacaoRepository
                .findByEvento_IdAndJogador_ContaId(evento.getId(), jogador.getContaId())
                .orElse(null);

        if (participacao != null && participacao.getStatus() == StatusParticipacao.CONFIRMADO) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Jogador já confirmado neste evento");
        }

        verificarVagas(evento);

        if (participacao == null) {
            participacao = new EventoParticipacao();
            participacao.setEvento(evento);
            participacao.setJogador(jogador);
        }

        participacao.setStatus(StatusParticipacao.CONFIRMADO);
        participacao.setCanceladoEm(null);

        return eventoParticipacaoRepository.save(participacao);
    }

    @Transactional
    public EventoParticipacao atualizar(Long id, EventoParticipacaoRequest request) {
        if (request.status() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Informe o status");
        }

        EventoParticipacao participacao = buscarPorId(id);
        verificarJogadorOuEquipe(participacao);

        if (request.status() == participacao.getStatus()) {
            return participacao;
        }

        if (request.status() == StatusParticipacao.CONFIRMADO) {
            verificarVagas(participacao.getEvento());
            participacao.setCanceladoEm(null);
        } else {
            participacao.setCanceladoEm(LocalDateTime.now());
        }

        participacao.setStatus(request.status());
        return eventoParticipacaoRepository.save(participacao);
    }

    @Transactional
    public void deletar(Long id) {
        EventoParticipacao participacao = buscarPorId(id);
        verificarJogadorOuEquipe(participacao);
        eventoParticipacaoRepository.delete(participacao);
    }

    // vagas_max NULL = sem limite
    private void verificarVagas(Evento evento) {
        if (evento.getVagasMax() != null
                && eventoParticipacaoRepository.countByEvento_IdAndStatus(
                        evento.getId(), StatusParticipacao.CONFIRMADO) >= evento.getVagasMax()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Não há mais vagas para este evento");
        }
    }

    private void verificarJogadorOuEquipe(EventoParticipacao participacao) {
        permissaoService.verificarContaOuEquipeLoja(
                participacao.getJogador().getContaId(), participacao.getEvento().getLoja().getContaId());
    }
}

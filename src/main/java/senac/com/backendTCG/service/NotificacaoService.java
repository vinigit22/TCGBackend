package senac.com.backendTCG.service;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import senac.com.backendTCG.dto.NotificacaoRequest;
import senac.com.backendTCG.entity.Conta;
import senac.com.backendTCG.entity.Evento;
import senac.com.backendTCG.entity.Notificacao;
import senac.com.backendTCG.entity.Torneio;
import senac.com.backendTCG.entity.enums.StatusInscricao;
import senac.com.backendTCG.entity.enums.StatusParticipacao;
import senac.com.backendTCG.entity.enums.TipoNotificacao;
import senac.com.backendTCG.repository.EventoParticipacaoRepository;
import senac.com.backendTCG.repository.InscricaoRepository;
import senac.com.backendTCG.repository.NotificacaoRepository;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class NotificacaoService {

    private final NotificacaoRepository notificacaoRepository;
    private final InscricaoRepository inscricaoRepository;
    private final EventoParticipacaoRepository eventoParticipacaoRepository;
    private final ContaService contaService;
    private final PermissaoService permissaoService;

    public List<Notificacao> listarMinhas() {
        Conta conta = permissaoService.contaLogada();
        return notificacaoRepository.findByConta_IdOrderByCriadoEmDesc(conta.getId());
    }

    public List<Notificacao> listarNaoLidas() {
        Conta conta = permissaoService.contaLogada();
        return notificacaoRepository.findByConta_IdAndLidaFalseOrderByCriadoEmDesc(conta.getId());
    }

    public long contarNaoLidas() {
        Conta conta = permissaoService.contaLogada();
        return notificacaoRepository.countByConta_IdAndLidaFalse(conta.getId());
    }

    public Notificacao buscarPorId(Long id) {
        Notificacao notificacao = notificacaoRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Notificação não encontrada"));

        permissaoService.verificarContaOuAdmin(notificacao.getConta().getId());
        return notificacao;
    }

    // Envio manual (aviso geral) feito por um administrador
    @Transactional
    public Notificacao criar(NotificacaoRequest request) {
        permissaoService.verificarAdmin();
        Conta destino = contaService.buscarPorId(request.contaId());

        return notificacaoRepository.save(nova(
                destino, request.tipo(), request.titulo(), request.mensagem(),
                request.torneioId(), request.eventoId(), request.partidaId()));
    }

    @Transactional
    public Notificacao marcarComoLida(Long id) {
        Notificacao notificacao = buscarPorId(id);

        if (!notificacao.getLida()) {
            notificacao.setLida(true);
            notificacao.setLidaEm(LocalDateTime.now());
        }

        return notificacaoRepository.save(notificacao);
    }

    @Transactional
    public void marcarTodasComoLidas() {
        List<Notificacao> naoLidas = listarNaoLidas();
        LocalDateTime agora = LocalDateTime.now();

        naoLidas.forEach(n -> {
            n.setLida(true);
            n.setLidaEm(agora);
        });

        notificacaoRepository.saveAll(naoLidas);
    }

    @Transactional
    public void deletar(Long id) {
        notificacaoRepository.delete(buscarPorId(id));
    }

    // ---- Notificacoes automaticas disparadas pelos outros services ----

    public Notificacao notificar(Conta conta, TipoNotificacao tipo, String titulo, String mensagem,
                                 Long torneioId, Long eventoId, Long partidaId) {
        return notificacaoRepository.save(nova(conta, tipo, titulo, mensagem, torneioId, eventoId, partidaId));
    }

    // Avisa quem ocupa vaga no torneio (INSCRITO ou CONFIRMADO)
    public void notificarInscritosTorneio(Torneio torneio, TipoNotificacao tipo, String titulo, String mensagem) {
        List<Notificacao> notificacoes = inscricaoRepository
                .findByTorneio_IdAndStatusIn(torneio.getId(),
                        List.of(StatusInscricao.INSCRITO, StatusInscricao.CONFIRMADO))
                .stream()
                .map(inscricao -> nova(inscricao.getJogador().getConta(), tipo, titulo, mensagem,
                        torneio.getId(), null, null))
                .toList();

        notificacaoRepository.saveAll(notificacoes);
    }

    public void notificarParticipantesEvento(Evento evento, String mensagem) {
        List<Notificacao> notificacoes = eventoParticipacaoRepository
                .findByEvento_IdAndStatus(evento.getId(), StatusParticipacao.CONFIRMADO)
                .stream()
                .map(participacao -> nova(participacao.getJogador().getConta(), TipoNotificacao.EVENTO_ATUALIZADO,
                        "Evento atualizado", mensagem, null, evento.getId(), null))
                .toList();

        notificacaoRepository.saveAll(notificacoes);
    }

    private Notificacao nova(Conta conta, TipoNotificacao tipo, String titulo, String mensagem,
                             Long torneioId, Long eventoId, Long partidaId) {
        Notificacao notificacao = new Notificacao();
        notificacao.setConta(conta);
        notificacao.setTipo(tipo);
        notificacao.setTitulo(titulo);
        notificacao.setMensagem(mensagem);
        notificacao.setTorneioId(torneioId);
        notificacao.setEventoId(eventoId);
        notificacao.setPartidaId(partidaId);
        return notificacao;
    }
}

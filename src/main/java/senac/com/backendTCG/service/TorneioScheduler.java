package senac.com.backendTCG.service;

import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import senac.com.backendTCG.entity.Conta;
import senac.com.backendTCG.entity.Inscricao;
import senac.com.backendTCG.entity.Torneio;
import senac.com.backendTCG.entity.enums.StatusTorneio;
import senac.com.backendTCG.entity.enums.TipoNotificacao;
import senac.com.backendTCG.repository.InscricaoRepository;
import senac.com.backendTCG.repository.NotificacaoRepository;
import senac.com.backendTCG.repository.TorneioRepository;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

// Tarefas por prazo, executadas a cada app.agendador.intervalo-ms:
//  - encerra as inscricoes dos torneios cujo prazo (inscricoes_ate) passou;
//  - lembra os inscritos quando faltam menos de 24h para o torneio (uma vez por jogador).
@Component
@RequiredArgsConstructor
public class TorneioScheduler {

    private static final String TITULO_LEMBRETE = "Lembrete de torneio";
    private static final DateTimeFormatter FORMATO_DATA = DateTimeFormatter.ofPattern("dd/MM 'às' HH:mm");

    private final TorneioRepository torneioRepository;
    private final InscricaoRepository inscricaoRepository;
    private final NotificacaoRepository notificacaoRepository;
    private final NotificacaoService notificacaoService;

    // Lembretes ja enviados ("contaId:torneioId") desde que a aplicacao subiu. O banco nao tem coluna de
    // "lembrete enviado" e a checagem pela propria notificacao falha se o jogador apaga-la; esta lista
    // cobre esse caso. So o agendador usa (sem execucoes simultaneas, por ser fixedDelay).
    private final Set<String> lembretesEnviados = new HashSet<>();

    @Scheduled(fixedDelayString = "${app.agendador.intervalo-ms}", initialDelayString = "${app.agendador.intervalo-ms}")
    @Transactional
    public void executar() {
        LocalDateTime agora = LocalDateTime.now();
        encerrarInscricoesVencidas(agora);
        lembrarTorneiosProximos(agora);
    }

    private void encerrarInscricoesVencidas(LocalDateTime agora) {
        List<Torneio> vencidos = torneioRepository.findByStatusAndInscricoesAteBeforeAndDeletadoEmIsNull(
                StatusTorneio.INSCRICOES_ABERTAS, agora);

        for (Torneio torneio : vencidos) {
            torneio.setStatus(StatusTorneio.INSCRICOES_ENCERRADAS);

            notificacaoService.notificar(torneio.getLoja().getConta(), TipoNotificacao.AVISO_GERAL,
                    "Inscrições encerradas",
                    "O prazo de inscrição do torneio '" + torneio.getTitulo()
                            + "' terminou. Faça o check-in dos jogadores e gere a chave.",
                    torneio.getId(), null, null);

            notificacaoService.notificarInscritosTorneio(torneio, TipoNotificacao.AVISO_GERAL,
                    "Inscrições encerradas",
                    "As inscrições do torneio '" + torneio.getTitulo() + "' foram encerradas.");
        }
    }

    private void lembrarTorneiosProximos(LocalDateTime agora) {
        List<Torneio> proximos = torneioRepository.findByStatusInAndDataInicioBetweenAndDeletadoEmIsNull(
                List.of(StatusTorneio.INSCRICOES_ABERTAS, StatusTorneio.INSCRICOES_ENCERRADAS),
                agora, agora.plusHours(24));

        Set<String> aindaNoPrazo = new HashSet<>();

        for (Torneio torneio : proximos) {
            for (Inscricao inscricao : inscricaoRepository.findByTorneio_IdAndStatusIn(
                    torneio.getId(), ListaEsperaService.OCUPAM_VAGA)) {
                Conta conta = inscricao.getJogador().getConta();
                String chave = conta.getId() + ":" + torneio.getId();
                aindaNoPrazo.add(chave);

                // Ja enviado nesta execucao da aplicacao, ou a notificacao ainda esta no banco (envio antes de reiniciar)
                if (lembretesEnviados.contains(chave) || notificacaoRepository.existsByConta_IdAndTorneioIdAndTitulo(
                        conta.getId(), torneio.getId(), TITULO_LEMBRETE)) {
                    lembretesEnviados.add(chave);
                    continue;
                }

                notificacaoService.notificar(conta, TipoNotificacao.AVISO_GERAL, TITULO_LEMBRETE,
                        "O torneio '" + torneio.getTitulo() + "' começa em "
                                + torneio.getDataInicio().format(FORMATO_DATA) + ". Não esqueça do check-in!",
                        torneio.getId(), null, null);
                lembretesEnviados.add(chave);
            }
        }

        // Torneios que ja comecaram (ou inscricoes canceladas) nao precisam mais ser lembrados
        lembretesEnviados.retainAll(aindaNoPrazo);
    }
}

package senac.com.backendTCG.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import senac.com.backendTCG.entity.Inscricao;
import senac.com.backendTCG.entity.Torneio;
import senac.com.backendTCG.entity.enums.StatusInscricao;
import senac.com.backendTCG.entity.enums.StatusTorneio;
import senac.com.backendTCG.entity.enums.TipoNotificacao;
import senac.com.backendTCG.repository.InscricaoRepository;

import java.util.List;
import java.util.Optional;
import java.util.Set;

// Controle de vagas e da lista de espera, usado pelas inscricoes (cancelamento) e pelo torneio (aumento de vagas)
@Service
@RequiredArgsConstructor
public class ListaEsperaService {

    // Status que contam como vaga ocupada (mesma regra da trigger trg_inscricao_controla_vagas)
    public static final List<StatusInscricao> OCUPAM_VAGA =
            List.of(StatusInscricao.INSCRITO, StatusInscricao.CONFIRMADO);

    // Depois da chave sorteada (ou com o torneio encerrado) ninguem mais sobe da lista de espera
    private static final Set<StatusTorneio> SEM_PROMOCAO =
            Set.of(StatusTorneio.EM_ANDAMENTO, StatusTorneio.FINALIZADO, StatusTorneio.CANCELADO);

    private final InscricaoRepository inscricaoRepository;
    private final NotificacaoService notificacaoService;

    public boolean temVaga(Torneio torneio) {
        return inscricaoRepository.countByTorneio_IdAndStatusIn(torneio.getId(), OCUPAM_VAGA) < torneio.getVagasMax();
    }

    // Enquanto houver vaga, o primeiro da lista de espera sobe para INSCRITO e e avisado
    public void promover(Torneio torneio) {
        if (SEM_PROMOCAO.contains(torneio.getStatus())) {
            return;
        }

        inscricaoRepository.flush();

        while (temVaga(torneio)) {
            Optional<Inscricao> proximo = inscricaoRepository
                    .findFirstByTorneio_IdAndStatusOrderByInscritoEmAsc(torneio.getId(), StatusInscricao.LISTA_ESPERA);
            if (proximo.isEmpty()) {
                return;
            }

            Inscricao inscricao = proximo.get();
            inscricao.setStatus(StatusInscricao.INSCRITO);
            inscricaoRepository.saveAndFlush(inscricao);

            notificacaoService.notificar(inscricao.getJogador().getConta(), TipoNotificacao.AVISO_GERAL,
                    "Você saiu da lista de espera",
                    "Abriu uma vaga no torneio '" + torneio.getTitulo() + "' e você está inscrito.",
                    torneio.getId(), null, null);
        }
    }
}

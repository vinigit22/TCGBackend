package senac.com.backendTCG.service;

import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import senac.com.backendTCG.dto.InscricaoRequest;
import senac.com.backendTCG.entity.Conta;
import senac.com.backendTCG.entity.Inscricao;
import senac.com.backendTCG.entity.Torneio;
import senac.com.backendTCG.entity.UsuarioJogador;
import senac.com.backendTCG.entity.enums.StatusInscricao;
import senac.com.backendTCG.entity.enums.StatusPagamento;
import senac.com.backendTCG.entity.enums.StatusTorneio;
import senac.com.backendTCG.entity.enums.TipoNotificacao;
import senac.com.backendTCG.repository.InscricaoRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class InscricaoService {

    private final InscricaoRepository inscricaoRepository;
    private final TorneioService torneioService;
    private final UsuarioJogadorService usuarioJogadorService;
    private final PermissaoService permissaoService;
    private final NotificacaoService notificacaoService;
    private final ListaEsperaService listaEsperaService;
    private final EntityManager entityManager;

    // Inscricoes nao sao publicas (tem status de pagamento): o jogador ve as proprias (jogadorId = a sua conta),
    // a equipe da loja ve as dos seus torneios (torneioId) e o admin ve todas
    public List<Inscricao> listar(Long torneioId, Long jogadorId, StatusInscricao status) {
        Conta conta = permissaoService.contaLogada();

        if (!permissaoService.isAdmin(conta) && !conta.getId().equals(jogadorId)) {
            if (torneioId == null) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                        "Informe jogadorId (suas inscrições) ou torneioId (equipe da loja)");
            }
            permissaoService.verificarEquipeLoja(torneioService.buscarPorId(torneioId).getLoja().getContaId());
        }

        return inscricaoRepository.filtrar(torneioId, jogadorId, status);
    }

    // GET /inscricoes/{id}: o dono da inscricao, a equipe da loja ou um admin
    public Inscricao consultar(Long id) {
        Inscricao inscricao = buscarPorId(id);
        permissaoService.verificarContaOuEquipeLoja(
                inscricao.getJogador().getContaId(), inscricao.getTorneio().getLoja().getContaId());
        return inscricao;
    }

    // Inscricoes de torneio excluido (soft delete) somem junto com ele
    public Inscricao buscarPorId(Long id) {
        return inscricaoRepository.findById(id)
                .filter(inscricao -> inscricao.getTorneio().getDeletadoEm() == null)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Inscrição não encontrada"));
    }

    @Transactional
    public Inscricao inscrever(InscricaoRequest request) {
        if (request.torneioId() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Informe o torneioId");
        }

        Torneio torneio = torneioService.buscarPorId(request.torneioId());
        UsuarioJogador jogador =
                usuarioJogadorService.resolverInscrito(request.jogadorId(), torneio.getLoja().getContaId());
        boolean propria = jogador.getContaId().equals(permissaoService.contaLogada().getId());

        if (propria) {
            // O proprio jogador so entra com inscricoes abertas e dentro do prazo
            if (torneio.getStatus() != StatusTorneio.INSCRICOES_ABERTAS) {
                throw new ResponseStatusException(HttpStatus.CONFLICT, "As inscrições deste torneio não estão abertas");
            }
            if (torneio.getInscricoesAte() != null && LocalDateTime.now().isAfter(torneio.getInscricoesAte())) {
                throw new ResponseStatusException(HttpStatus.CONFLICT, "O prazo de inscrição deste torneio já terminou");
            }
        } else if (torneio.getStatus() != StatusTorneio.RASCUNHO
                && torneio.getStatus() != StatusTorneio.INSCRICOES_ABERTAS) {
            // A equipe da loja pode inscrever tambem em rascunho (igual a trigger)
            throw new ResponseStatusException(HttpStatus.CONFLICT, "As inscrições deste torneio não estão abertas");
        }

        StatusPagamento pagamento = torneio.getTaxaInscricao().signum() == 0
                ? StatusPagamento.ISENTO
                : StatusPagamento.PENDENTE;

        Optional<Inscricao> existente =
                inscricaoRepository.findByTorneio_IdAndJogador_ContaId(torneio.getId(), jogador.getContaId());

        if (existente.isPresent()) {
            Inscricao inscricao = existente.get();

            if (inscricao.getStatus() != StatusInscricao.CANCELADO) {
                throw new ResponseStatusException(HttpStatus.CONFLICT, "Jogador já inscrito neste torneio");
            }

            // Reinscricao: a trigger de vagas so roda no INSERT, entao a regra e aplicada aqui
            inscricao.setStatus(listaEsperaService.temVaga(torneio) ? StatusInscricao.INSCRITO : StatusInscricao.LISTA_ESPERA);
            inscricao.setPagamentoStatus(pagamento);
            inscricao.setInscritoEm(LocalDateTime.now());
            inscricao.setCheckInEm(null);
            inscricao.setCanceladoEm(null);
            return inscricaoRepository.save(inscricao);
        }

        Inscricao inscricao = new Inscricao();
        inscricao.setTorneio(torneio);
        inscricao.setJogador(jogador);
        inscricao.setPagamentoStatus(pagamento);
        inscricao.setStatus(listaEsperaService.temVaga(torneio) ? StatusInscricao.INSCRITO : StatusInscricao.LISTA_ESPERA);

        inscricaoRepository.saveAndFlush(inscricao);

        // A trigger trg_inscricao_controla_vagas tem a palavra final sobre INSCRITO x LISTA_ESPERA
        entityManager.refresh(inscricao);
        return inscricao;
    }

    // Edicao feita pela equipe da loja (status, pagamento e seed)
    @Transactional
    public Inscricao atualizar(Long id, InscricaoRequest request) {
        Inscricao inscricao = buscarPorId(id);
        torneioService.verificarPodeGerenciar(inscricao.getTorneio());

        if (request.pagamentoStatus() != null) {
            inscricao.setPagamentoStatus(request.pagamentoStatus());
        }

        if (request.seed() != null) {
            if (inscricaoRepository.existsByTorneio_IdAndSeedAndIdNot(
                    inscricao.getTorneio().getId(), request.seed(), id)) {
                throw new ResponseStatusException(HttpStatus.CONFLICT, "Seed já usado por outra inscrição deste torneio");
            }
            inscricao.setSeed(request.seed());
        }

        if (request.status() != null && request.status() != inscricao.getStatus()) {
            aplicarStatus(inscricao, request.status());
        }

        return inscricaoRepository.save(inscricao);
    }

    // Check-in no dia do torneio: so inscricoes CONFIRMADAS entram na chave
    @Transactional
    public Inscricao checkIn(Long id) {
        Inscricao inscricao = buscarPorId(id);
        torneioService.verificarPodeGerenciar(inscricao.getTorneio());

        if (inscricao.getStatus() == StatusInscricao.CONFIRMADO) {
            return inscricao;
        }

        if (inscricao.getStatus() == StatusInscricao.CANCELADO || inscricao.getStatus() == StatusInscricao.NO_SHOW) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Esta inscrição não pode fazer check-in");
        }

        aplicarStatus(inscricao, StatusInscricao.CONFIRMADO);
        return inscricaoRepository.save(inscricao);
    }

    // Desinscricao: o proprio jogador ou a equipe da loja
    @Transactional
    public Inscricao cancelar(Long id) {
        Inscricao inscricao = buscarPorId(id);
        Torneio torneio = inscricao.getTorneio();
        permissaoService.verificarContaOuEquipeLoja(inscricao.getJogador().getContaId(), torneio.getLoja().getContaId());

        if (torneio.getStatus() == StatusTorneio.EM_ANDAMENTO || torneio.getStatus() == StatusTorneio.FINALIZADO) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT, "Não é possível cancelar a inscrição depois que o torneio começou");
        }

        if (inscricao.getStatus() == StatusInscricao.CANCELADO) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Inscrição já cancelada");
        }

        aplicarStatus(inscricao, StatusInscricao.CANCELADO);
        return inscricaoRepository.save(inscricao);
    }

    // Exclusao definitiva. Inscricao usada em partida nao pode ser apagada (FK) -> 409.
    @Transactional
    public void deletar(Long id) {
        Inscricao inscricao = buscarPorId(id);
        torneioService.verificarPodeGerenciar(inscricao.getTorneio());

        inscricaoRepository.delete(inscricao);
        inscricaoRepository.flush();
    }

    private void aplicarStatus(Inscricao inscricao, StatusInscricao novo) {
        StatusTorneio statusTorneio = inscricao.getTorneio().getStatus();

        // Depois do sorteio, quem nao esta na chave nao entra mais (o status NO_SHOW e marcado pela propria chave)
        if (statusTorneio == StatusTorneio.EM_ANDAMENTO) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "A chave deste torneio já foi gerada: as inscrições não podem mais mudar de status");
        }
        if (novo == StatusInscricao.CONFIRMADO && statusTorneio == StatusTorneio.CANCELADO) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Este torneio foi cancelado");
        }

        StatusInscricao anterior = inscricao.getStatus();
        boolean ocupava = ListaEsperaService.OCUPAM_VAGA.contains(anterior);
        boolean vaiOcupar = ListaEsperaService.OCUPAM_VAGA.contains(novo);

        if (vaiOcupar && !ocupava && !listaEsperaService.temVaga(inscricao.getTorneio())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Não há vagas disponíveis neste torneio");
        }

        inscricao.setStatus(novo);

        if (novo == StatusInscricao.CANCELADO) {
            // a trigger trg_inscricao_cancelamento faz o mesmo no banco
            inscricao.setCanceladoEm(LocalDateTime.now());
            inscricao.setSeed(null);
        } else {
            inscricao.setCanceladoEm(null);
        }

        if (novo == StatusInscricao.CONFIRMADO) {
            if (inscricao.getCheckInEm() == null) {
                inscricao.setCheckInEm(LocalDateTime.now());
            }

            notificacaoService.notificar(inscricao.getJogador().getConta(), TipoNotificacao.INSCRICAO_CONFIRMADA,
                    "Inscrição confirmada",
                    "Sua inscrição no torneio '" + inscricao.getTorneio().getTitulo() + "' foi confirmada.",
                    inscricao.getTorneio().getId(), null, null);
        }

        // Vaga liberada: o primeiro da lista de espera sobe para INSCRITO
        if (ocupava && !vaiOcupar) {
            listaEsperaService.promover(inscricao.getTorneio());
        }
    }
}

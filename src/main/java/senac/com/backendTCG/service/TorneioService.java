package senac.com.backendTCG.service;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.DataClassRowMapper;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import senac.com.backendTCG.dto.ChaveamentoResponse;
import senac.com.backendTCG.dto.TorneioRequest;
import senac.com.backendTCG.dto.TorneioVagasResponse;
import senac.com.backendTCG.entity.Formato;
import senac.com.backendTCG.entity.Jogo;
import senac.com.backendTCG.entity.Torneio;
import senac.com.backendTCG.entity.UsuarioLoja;
import senac.com.backendTCG.entity.enums.StatusInscricao;
import senac.com.backendTCG.entity.enums.StatusTorneio;
import senac.com.backendTCG.entity.enums.TipoNotificacao;
import senac.com.backendTCG.repository.InscricaoRepository;
import senac.com.backendTCG.repository.TorneioRepository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class TorneioService {

    // Mesma regra do CHECK ck_torneio_vagas
    private static final Set<Integer> VAGAS_PERMITIDAS = Set.of(2, 4, 8, 16, 32, 64, 128, 256);

    // Mudancas permitidas em PUT /torneios/{id}/status. EM_ANDAMENTO so e alcancado gerando a chave,
    // e FINALIZADO, registrando o resultado da final.
    private static final Map<StatusTorneio, Set<StatusTorneio>> TRANSICOES = Map.of(
            StatusTorneio.RASCUNHO, Set.of(StatusTorneio.INSCRICOES_ABERTAS, StatusTorneio.CANCELADO),
            StatusTorneio.INSCRICOES_ABERTAS, Set.of(StatusTorneio.RASCUNHO, StatusTorneio.INSCRICOES_ENCERRADAS,
                    StatusTorneio.CANCELADO),
            StatusTorneio.INSCRICOES_ENCERRADAS, Set.of(StatusTorneio.INSCRICOES_ABERTAS, StatusTorneio.CANCELADO),
            StatusTorneio.EM_ANDAMENTO, Set.of(StatusTorneio.CANCELADO),
            StatusTorneio.FINALIZADO, Set.of(),
            StatusTorneio.CANCELADO, Set.of());

    private final TorneioRepository torneioRepository;
    private final InscricaoRepository inscricaoRepository;
    private final UsuarioLojaService usuarioLojaService;
    private final JogoService jogoService;
    private final FormatoService formatoService;
    private final EnderecoService enderecoService;
    private final PermissaoService permissaoService;
    private final NotificacaoService notificacaoService;
    private final ChaveamentoService chaveamentoService;
    private final JdbcTemplate jdbcTemplate;

    public List<Torneio> listar(Long lojaId, Integer jogoId, StatusTorneio status) {
        return torneioRepository.filtrar(lojaId, jogoId, status);
    }

    public Torneio buscarPorId(Long id) {
        return torneioRepository.findByIdAndDeletadoEmIsNull(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Torneio não encontrado"));
    }

    @Transactional
    public Torneio criar(TorneioRequest request) {
        if (request.lojaId() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Informe o lojaId");
        }

        UsuarioLoja loja = usuarioLojaService.buscarPorId(request.lojaId());
        permissaoService.verificarEquipeLoja(loja.getContaId());

        Torneio torneio = new Torneio();
        torneio.setLoja(loja);
        preencher(torneio, request);

        return torneioRepository.save(torneio);
    }

    @Transactional
    public Torneio atualizar(Long id, TorneioRequest request) {
        Torneio torneio = buscarPorId(id);
        verificarPodeGerenciar(torneio);

        long ocupadas = inscricaoRepository.countByTorneio_IdAndStatusIn(
                id, List.of(StatusInscricao.INSCRITO, StatusInscricao.CONFIRMADO));

        if (request.vagasMax() < ocupadas) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "O torneio já tem " + ocupadas + " inscritos; vagasMax não pode ser menor que isso");
        }

        preencher(torneio, request);
        return torneioRepository.save(torneio);
    }

    @Transactional
    public Torneio alterarStatus(Long id, StatusTorneio status) {
        Torneio torneio = buscarPorId(id);
        verificarPodeGerenciar(torneio);

        if (torneio.getStatus() == status) {
            return torneio;
        }

        if (!TRANSICOES.get(torneio.getStatus()).contains(status)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, mensagemTransicaoInvalida(torneio.getStatus(), status));
        }

        torneio.setStatus(status);
        Torneio salvo = torneioRepository.save(torneio);

        if (status == StatusTorneio.CANCELADO) {
            notificacaoService.notificarInscritosTorneio(salvo, TipoNotificacao.TORNEIO_CANCELADO,
                    "Torneio cancelado", "O torneio '" + salvo.getTitulo() + "' foi cancelado pela loja.");
        }
        return salvo;
    }

    // Soft delete (coluna deletado_em). Torneio finalizado nao pode ser excluido.
    @Transactional
    public void deletar(Long id) {
        Torneio torneio = buscarPorId(id);
        verificarPodeGerenciar(torneio);

        torneio.setDeletadoEm(LocalDateTime.now());
        torneioRepository.save(torneio);
    }

    // Sorteia os CONFIRMADOS (com byes quando nao fecham uma potencia de 2), cria rodadas e partidas
    // e muda o torneio para EM_ANDAMENTO
    @Transactional
    public List<ChaveamentoResponse> gerarChaveamento(Long id) {
        Torneio torneio = buscarPorId(id);
        verificarPodeGerenciar(torneio);

        chaveamentoService.gerar(torneio);
        return listarChaveamento(id);
    }

    // Chave pronta para exibicao (view vw_chaveamento)
    public List<ChaveamentoResponse> listarChaveamento(Long id) {
        buscarPorId(id);

        return jdbcTemplate.query("""
                        SELECT torneio_id, rodada, nome_rodada, partida_id, mesa, jogador_a, jogador_b,
                               games_a, games_b, resultado, vencedor, status
                          FROM vw_chaveamento
                         WHERE torneio_id = ?
                         ORDER BY rodada, mesa
                        """,
                new DataClassRowMapper<>(ChaveamentoResponse.class), id);
    }

    // Vagas restantes, lista de espera e pagamentos pendentes (view vw_torneio_vagas)
    public TorneioVagasResponse consultarVagas(Long id) {
        buscarPorId(id);

        return jdbcTemplate.queryForObject("""
                        SELECT torneio_id, titulo, vagas_max, inscritos, vagas_restantes,
                               lista_espera, pagamentos_pendentes
                          FROM vw_torneio_vagas
                         WHERE torneio_id = ?
                        """,
                new DataClassRowMapper<>(TorneioVagasResponse.class), id);
    }

    // Equipe da loja + torneio ainda editavel. Usado tambem por rodadas, partidas, games e inscricoes.
    public void verificarPodeGerenciar(Torneio torneio) {
        permissaoService.verificarEquipeLoja(torneio.getLoja().getContaId());
        verificarEditavel(torneio);
    }

    // Mesma regra das triggers trg_torneio_bloqueia_update / trg_torneio_bloqueia_delete
    private void verificarEditavel(Torneio torneio) {
        if (torneio.getStatus() == StatusTorneio.FINALIZADO) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Torneio finalizado não pode ser alterado");
        }
    }

    private void preencher(Torneio torneio, TorneioRequest request) {
        if (!VAGAS_PERMITIDAS.contains(request.vagasMax())) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST, "vagasMax deve ser 2, 4, 8, 16, 32, 64, 128 ou 256");
        }

        // Mesma regra do CHECK ck_torneio_prazo
        if (request.inscricoesAte() != null && request.inscricoesAte().isAfter(request.dataInicio())) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST, "O prazo de inscrição não pode ser depois do início do torneio");
        }

        Jogo jogo = jogoService.buscarPorId(request.jogoId());

        Formato formato = null;
        if (request.formatoId() != null) {
            formato = formatoService.buscarPorId(request.formatoId());

            if (!formato.getJogo().getId().equals(jogo.getId())) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "O formato informado não pertence a este jogo");
            }
        }

        torneio.setJogo(jogo);
        torneio.setFormato(formato);
        torneio.setTitulo(request.titulo());
        torneio.setDescricao(request.descricao());
        torneio.setImagem(request.imagem());
        torneio.setVagasMax(request.vagasMax());
        torneio.setTaxaInscricao(request.taxaInscricao() == null ? BigDecimal.ZERO : request.taxaInscricao());
        torneio.setPremiacao(request.premiacao());
        torneio.setEndereco(request.enderecoId() == null ? null : enderecoService.buscarPorId(request.enderecoId()));
        torneio.setInscricoesAte(request.inscricoesAte());
        torneio.setDataInicio(request.dataInicio());
    }

    private String mensagemTransicaoInvalida(StatusTorneio atual, StatusTorneio novo) {
        if (novo == StatusTorneio.EM_ANDAMENTO) {
            return "O torneio entra em andamento ao gerar a chave (POST /torneios/{id}/chaveamento).";
        }
        if (novo == StatusTorneio.FINALIZADO) {
            return "O torneio é finalizado automaticamente ao registrar o resultado da final.";
        }
        return "Não é possível mudar o torneio de " + atual + " para " + novo
                + ". Permitido a partir de " + atual + ": " + TRANSICOES.get(atual) + ".";
    }
}

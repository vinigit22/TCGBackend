package senac.com.backendTCG;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.transaction.annotation.Transactional;
import senac.com.backendTCG.entity.Inscricao;
import senac.com.backendTCG.entity.Partida;
import senac.com.backendTCG.entity.enums.ResultadoPartida;
import senac.com.backendTCG.entity.enums.StatusInscricao;
import senac.com.backendTCG.entity.enums.StatusPagamento;
import senac.com.backendTCG.entity.enums.StatusPartida;
import senac.com.backendTCG.entity.enums.StatusTorneio;
import senac.com.backendTCG.repository.InscricaoRepository;
import senac.com.backendTCG.repository.PartidaRepository;
import senac.com.backendTCG.repository.TorneioRepository;
import senac.com.backendTCG.service.ChaveamentoService;

import java.time.LocalDateTime;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// Fluxo do torneio de ponta a ponta contra o H2 com os dados de teste (resources/h2/data.sql).
// Torneio 5 (Magic Modern Showdown, Dragon's Lair): inscricoes encerradas, 4 confirmados e pagos.
// Cada teste roda numa transacao que e desfeita no fim, entao um nao interfere no outro.
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class FluxoTorneioTest {

    private static final long TORNEIO = 5L;
    private static final String LOJA = "contato@dragonslair.com.br";
    private static final String ADMIN = "admin@tcg.com";

    @Autowired MockMvc mvc;
    @Autowired TorneioRepository torneioRepository;
    @Autowired InscricaoRepository inscricaoRepository;
    @Autowired PartidaRepository partidaRepository;
    @Autowired ChaveamentoService chaveamentoService;

    // ---------------------------------------------------------------- chave e pagamento

    @Test
    void geraChaveCom4ConfirmadosPagos() throws Exception {
        gerarChave();

        assertEquals(StatusTorneio.EM_ANDAMENTO, torneioRepository.findById(TORNEIO).orElseThrow().getStatus());
        // 4 jogadores = 2 semifinais + 1 final
        assertEquals(3, partidas().size());
        assertEquals(2, primeiraRodada().size());
        assertTrue(primeiraRodada().stream().allMatch(p -> p.getInscricaoA() != null && p.getInscricaoB() != null));
    }

    @Test
    void quemNaoPagouNaoEntraNaChave() throws Exception {
        Inscricao devedor = inscricaoRepository.findByTorneio_IdAndStatusIn(TORNEIO, List.of(StatusInscricao.CONFIRMADO)).get(0);
        devedor.setPagamentoStatus(StatusPagamento.PENDENTE);
        inscricaoRepository.saveAndFlush(devedor);

        mvc.perform(post("/torneios/" + TORNEIO + "/chaveamento").header("Authorization", bearer(LOJA)))
                .andExpect(status().isConflict());

        assertTrue(partidas().isEmpty());
        assertEquals(StatusTorneio.INSCRICOES_ENCERRADAS, torneioRepository.findById(TORNEIO).orElseThrow().getStatus());
    }

    @Test
    void checkInDeInscricaoComPagamentoPendenteERecusado() throws Exception {
        // Torneio 2 (Pokemon, Card House): jogadores 2 e 3 inscritos com pagamento pendente
        Inscricao pendente = inscricaoRepository.findByTorneio_IdAndStatusIn(2L, List.of(StatusInscricao.INSCRITO)).get(0);
        assertEquals(StatusPagamento.PENDENTE, pendente.getPagamentoStatus());

        mvc.perform(put("/inscricoes/" + pendente.getId() + "/check-in").header("Authorization", bearer("contato@cardhouse.com.br")))
                .andExpect(status().isConflict());
    }

    // ---------------------------------------------------------------- check-in da partida

    @Test
    void resultadoSoDepoisDoCheckInDosDoisJogadores() throws Exception {
        Partida partida = gerarEConvocar();

        // Convocada, mas ninguem confirmou: a loja ainda nao pode lancar resultado
        lancarResultado(partida, "VITORIA_A").andExpect(status().isConflict());

        // So um confirmou: continua barrado
        confirmar(partida, partida.getInscricaoA());
        lancarResultado(partida, "VITORIA_A").andExpect(status().isConflict());
    }

    @Test
    void comOsDoisConfirmadosAPartidaComecaSozinha() throws Exception {
        Partida partida = gerarEConvocar();

        confirmar(partida, partida.getInscricaoA());
        confirmar(partida, partida.getInscricaoB());

        Partida atual = partidaRepository.findById(partida.getId()).orElseThrow();
        assertEquals(StatusPartida.EM_ANDAMENTO, atual.getStatus());
        assertNotNull(atual.getIniciadaEm());

        // E agora a loja pode lancar o resultado
        lancarResultado(atual, "VITORIA_A").andExpect(status().isOk());
    }

    @Test
    void soUmConfirmouGanhaPorWoEAvanca() throws Exception {
        Partida partida = gerarEConvocar();
        Inscricao presente = partida.getInscricaoA();
        Inscricao ausente = partida.getInscricaoB();

        confirmar(partida, presente);
        vencerPrazoDeCheckIn(partida);

        assertTrue(chaveamentoService.encerrarPrazoDeCheckIn(partida));

        assertEquals(StatusPartida.FINALIZADA, partida.getStatus());
        assertEquals(ResultadoPartida.WO_A, partida.getResultado());
        assertEquals(presente.getId(), partida.getVencedor().getId());
        assertEquals(StatusInscricao.NO_SHOW, ausente.getStatus());

        // O vencedor ja esta na final
        Partida final_ = partidaRepository.findById(partida.getProximaPartidaId()).orElseThrow();
        assertTrue(presente.getId().equals(idOuNulo(final_.getInscricaoA()))
                || presente.getId().equals(idOuNulo(final_.getInscricaoB())));
    }

    @Test
    void ninguemConfirmouMesaFicaSemVencedor() throws Exception {
        Partida partida = gerarEConvocar();

        vencerPrazoDeCheckIn(partida);

        assertTrue(chaveamentoService.encerrarPrazoDeCheckIn(partida));

        assertEquals(StatusPartida.FINALIZADA, partida.getStatus());
        assertEquals(ResultadoPartida.DUPLO_NO_SHOW, partida.getResultado());
        assertNull(partida.getVencedor());

        // Ninguem foi para a final por esta mesa
        Partida final_ = partidaRepository.findById(partida.getProximaPartidaId()).orElseThrow();
        assertTrue(final_.getInscricaoA() == null && final_.getInscricaoB() == null);
    }

    @Test
    void checkInDeQuemNaoJogaNaMesaEProibido() throws Exception {
        Partida partida = gerarEConvocar();
        Partida outraMesa = primeiraRodada().stream()
                .filter(p -> !p.getId().equals(partida.getId())).findFirst().orElseThrow();

        mvc.perform(post("/partidas/" + partida.getId() + "/check-in")
                        .header("Authorization", bearer(emailDe(outraMesa.getInscricaoA()))))
                .andExpect(status().isForbidden());
    }

    // ---------------------------------------------------------------- paginacao

    @Test
    void listagemDePartidasSemPaginacaoVemInteira() throws Exception {
        gerarChave();

        mvc.perform(get("/partidas").param("torneioId", String.valueOf(TORNEIO)))
                .andExpect(status().isOk())
                .andExpect(header().doesNotExist("X-Total-Count"))
                .andExpect(jsonPath("$.length()").value(3));
    }

    @Test
    void listagemDePartidasPaginada() throws Exception {
        gerarChave();

        mvc.perform(get("/partidas").param("torneioId", String.valueOf(TORNEIO)).param("page", "0").param("size", "2"))
                .andExpect(status().isOk())
                .andExpect(header().string("X-Total-Count", "3"))
                .andExpect(jsonPath("$.length()").value(2));

        mvc.perform(get("/partidas").param("torneioId", String.valueOf(TORNEIO)).param("page", "1").param("size", "2"))
                .andExpect(header().string("X-Total-Count", "3"))
                .andExpect(jsonPath("$.length()").value(1));

        // Alem da ultima pagina: lista vazia, nao erro
        mvc.perform(get("/partidas").param("torneioId", String.valueOf(TORNEIO)).param("page", "9").param("size", "2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    void listagemDeContasPaginadaEParametrosInvalidos() throws Exception {
        String admin = bearer(ADMIN);

        mvc.perform(get("/contas").header("Authorization", admin).param("page", "0").param("size", "2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(header().exists("X-Total-Count"));

        mvc.perform(get("/contas").header("Authorization", admin).param("page", "-1"))
                .andExpect(status().isBadRequest());
        mvc.perform(get("/contas").header("Authorization", admin).param("size", "1000"))
                .andExpect(status().isBadRequest());

        // Continua restrito a admin
        mvc.perform(get("/contas").header("Authorization", bearer("eric@email.com")).param("page", "0"))
                .andExpect(status().isForbidden());
    }

    // ---------------------------------------------------------------- apoio

    private void gerarChave() throws Exception {
        mvc.perform(post("/torneios/" + TORNEIO + "/chaveamento").header("Authorization", bearer(LOJA)))
                .andExpect(status().is2xxSuccessful());
    }

    private Partida gerarEConvocar() throws Exception {
        gerarChave();
        Partida partida = primeiraRodada().get(0);
        mvc.perform(post("/partidas/" + partida.getId() + "/convocar").header("Authorization", bearer(LOJA)))
                .andExpect(status().isOk());
        return partidaRepository.findById(partida.getId()).orElseThrow();
    }

    private void confirmar(Partida partida, Inscricao inscricao) throws Exception {
        mvc.perform(post("/partidas/" + partida.getId() + "/check-in")
                        .header("Authorization", bearer(emailDe(inscricao))))
                .andExpect(status().isOk());
    }

    private ResultActions lancarResultado(Partida partida, String resultado) throws Exception {
        return mvc.perform(post("/partidas/" + partida.getId() + "/resultado")
                .header("Authorization", bearer(LOJA))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"gamesA\":2,\"gamesB\":0,\"resultado\":\"" + resultado + "\"}"));
    }

    // Em vez de esperar 5 minutos de verdade
    private void vencerPrazoDeCheckIn(Partida partida) {
        partida.setCheckInExpiraEm(LocalDateTime.now().minusSeconds(1));
        partidaRepository.saveAndFlush(partida);
    }

    private List<Partida> partidas() {
        return partidaRepository.findByRodada_Torneio_IdOrderByRodada_NumeroAscMesaAsc(TORNEIO);
    }

    private List<Partida> primeiraRodada() {
        return partidas().stream().filter(p -> p.getRodada().getNumero() == 1).toList();
    }

    private String emailDe(Inscricao inscricao) {
        return inscricao.getJogador().getConta().getEmail();
    }

    private Long idOuNulo(Inscricao inscricao) {
        return inscricao == null ? null : inscricao.getId();
    }

    private String bearer(String email) throws Exception {
        String senha = email.equals(ADMIN) ? "admin123" : "123456";
        String resposta = mvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"" + email + "\",\"senha\":\"" + senha + "\"}"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        Matcher token = Pattern.compile("\"token\"\s*:\s*\"([^\"]+)\"").matcher(resposta);
        assertTrue(token.find(), "login sem token: " + resposta);
        return "Bearer " + token.group(1);
    }
}

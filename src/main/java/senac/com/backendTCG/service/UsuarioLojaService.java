package senac.com.backendTCG.service;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.DataClassRowMapper;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import senac.com.backendTCG.dto.AgendaLojaResponse;
import senac.com.backendTCG.dto.RegistroLojaRequest;
import senac.com.backendTCG.dto.UsuarioLojaRequest;
import senac.com.backendTCG.entity.Conta;
import senac.com.backendTCG.entity.LojaMembro;
import senac.com.backendTCG.entity.UsuarioLoja;
import senac.com.backendTCG.entity.enums.PapelMembro;
import senac.com.backendTCG.entity.enums.TipoConta;
import senac.com.backendTCG.repository.LojaMembroRepository;
import senac.com.backendTCG.repository.UsuarioLojaRepository;
import senac.com.backendTCG.util.SlugUtils;

import java.util.List;

@Service
@RequiredArgsConstructor
public class UsuarioLojaService {

    private final UsuarioLojaRepository usuarioLojaRepository;
    private final LojaMembroRepository lojaMembroRepository;
    private final ContaService contaService;
    private final EnderecoService enderecoService;
    private final PermissaoService permissaoService;
    private final JdbcTemplate jdbcTemplate;

    public List<UsuarioLoja> listarTodos() {
        return usuarioLojaRepository.findByConta_DeletadoEmIsNullOrderByNomeAsc();
    }

    public UsuarioLoja buscarPorId(Long id) {
        return usuarioLojaRepository.findById(id)
                .filter(loja -> loja.getConta().getDeletadoEm() == null)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Loja não encontrada"));
    }

    public UsuarioLoja buscarPorSlug(String slug) {
        return usuarioLojaRepository.findBySlugAndConta_DeletadoEmIsNull(slug)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Loja não encontrada"));
    }

    // Cria a conta (tipo LOJA), o perfil e o vinculo de PROPRIETARIO na mesma transacao
    @Transactional
    public UsuarioLoja criar(RegistroLojaRequest request) {
        String slug = gerarSlug(request.slug(), request.nome());

        if (usuarioLojaRepository.existsBySlug(slug)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Slug já cadastrado");
        }

        Conta conta = contaService.criar(request.email(), request.senha(), TipoConta.LOJA);

        UsuarioLoja loja = new UsuarioLoja();
        loja.setConta(conta);
        loja.setNome(request.nome());
        loja.setSlug(slug);
        loja.setDescricao(request.descricao());
        loja.setImagemPerfil(request.imagemPerfil());
        loja.setImagemBanner(request.imagemBanner());
        loja.setTelefone(request.telefone());
        loja = usuarioLojaRepository.save(loja);

        LojaMembro proprietario = new LojaMembro();
        proprietario.setLoja(loja);
        proprietario.setConta(conta);
        proprietario.setPapel(PapelMembro.PROPRIETARIO);
        lojaMembroRepository.save(proprietario);

        return loja;
    }

    @Transactional
    public UsuarioLoja atualizar(Long id, UsuarioLojaRequest request) {
        permissaoService.verificarProprietarioLoja(id);
        UsuarioLoja loja = buscarPorId(id);

        // Slug vazio na edicao = mantem o atual
        String slug = request.slug() == null || request.slug().isBlank()
                ? loja.getSlug()
                : gerarSlug(request.slug(), request.nome());

        if (usuarioLojaRepository.existsBySlugAndContaIdNot(slug, id)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Slug já cadastrado");
        }

        loja.setNome(request.nome());
        loja.setSlug(slug);
        loja.setDescricao(request.descricao());
        loja.setImagemPerfil(request.imagemPerfil());
        loja.setImagemBanner(request.imagemBanner());
        loja.setTelefone(request.telefone());
        loja.setEndereco(request.enderecoId() == null ? null : enderecoService.buscarPorId(request.enderecoId()));

        return usuarioLojaRepository.save(loja);
    }

    @Transactional
    public UsuarioLoja alterarVerificacao(Long id, Boolean verificada) {
        permissaoService.verificarAdmin();

        UsuarioLoja loja = buscarPorId(id);
        loja.setVerificada(verificada);
        return usuarioLojaRepository.save(loja);
    }

    // Soft delete da conta: torneios e eventos antigos continuam no banco
    @Transactional
    public void deletar(Long id) {
        buscarPorId(id);
        contaService.deletar(id);
    }

    // Eventos + torneios da loja (view vw_agenda_loja)
    public List<AgendaLojaResponse> listarAgenda(Long id) {
        buscarPorId(id);

        return jdbcTemplate.query("""
                        SELECT categoria, id, loja_id, titulo, imagem, data_inicio, status, jogo
                          FROM vw_agenda_loja
                         WHERE loja_id = ?
                         ORDER BY data_inicio
                        """,
                new DataClassRowMapper<>(AgendaLojaResponse.class), id);
    }

    private String gerarSlug(String slug, String nome) {
        String gerado = SlugUtils.gerar(slug == null || slug.isBlank() ? nome : slug);

        if (gerado.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Não foi possível gerar um slug válido");
        }

        return gerado;
    }
}

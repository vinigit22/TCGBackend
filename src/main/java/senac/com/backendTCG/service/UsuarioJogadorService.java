package senac.com.backendTCG.service;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.DataClassRowMapper;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import senac.com.backendTCG.dto.RegistroJogadorRequest;
import senac.com.backendTCG.dto.TrofeusResponse;
import senac.com.backendTCG.dto.UsuarioJogadorRequest;
import senac.com.backendTCG.entity.Conta;
import senac.com.backendTCG.entity.UsuarioJogador;
import senac.com.backendTCG.entity.enums.TipoConta;
import senac.com.backendTCG.repository.UsuarioJogadorRepository;

import java.util.List;
import java.util.Locale;

@Service
@RequiredArgsConstructor
public class UsuarioJogadorService {

    private final UsuarioJogadorRepository usuarioJogadorRepository;
    private final ContaService contaService;
    private final PermissaoService permissaoService;
    private final JdbcTemplate jdbcTemplate;

    public List<UsuarioJogador> listarTodos() {
        return usuarioJogadorRepository.findByConta_DeletadoEmIsNullOrderByNicknameAsc();
    }

    public UsuarioJogador buscarPorId(Long id) {
        return usuarioJogadorRepository.findById(id)
                .filter(jogador -> jogador.getConta().getDeletadoEm() == null)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Jogador não encontrado"));
    }

    public UsuarioJogador buscarPorNickname(String nickname) {
        return usuarioJogadorRepository.findByNicknameAndConta_DeletadoEmIsNull(nickname)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Jogador não encontrado"));
    }

    // Cria a conta (tipo JOGADOR) e o perfil na mesma transacao
    @Transactional
    public UsuarioJogador criar(RegistroJogadorRequest request) {
        if (usuarioJogadorRepository.existsByNickname(request.nickname())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Nickname já cadastrado");
        }

        Conta conta = contaService.criar(request.email(), request.senha(), TipoConta.JOGADOR);

        UsuarioJogador jogador = new UsuarioJogador();
        jogador.setConta(conta);
        jogador.setNome(request.nome());
        jogador.setNickname(request.nickname());
        jogador.setImagemPerfil(request.imagemPerfil());
        jogador.setBio(request.bio());
        jogador.setDataNascimento(request.dataNascimento());
        jogador.setCidade(request.cidade());
        jogador.setEstado(maiusculo(request.estado()));

        return usuarioJogadorRepository.save(jogador);
    }

    @Transactional
    public UsuarioJogador atualizar(Long id, UsuarioJogadorRequest request) {
        permissaoService.verificarContaOuAdmin(id);
        UsuarioJogador jogador = buscarPorId(id);

        if (usuarioJogadorRepository.existsByNicknameAndContaIdNot(request.nickname(), id)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Nickname já cadastrado");
        }

        jogador.setNome(request.nome());
        jogador.setNickname(request.nickname());
        jogador.setImagemPerfil(request.imagemPerfil());
        jogador.setBio(request.bio());
        jogador.setDataNascimento(request.dataNascimento());
        jogador.setCidade(request.cidade());
        jogador.setEstado(maiusculo(request.estado()));

        return usuarioJogadorRepository.save(jogador);
    }

    // Soft delete da conta: o historico de inscricoes e resultados continua no banco
    @Transactional
    public void deletar(Long id) {
        buscarPorId(id);
        contaService.deletar(id);
    }

    // Pagina de trofeus (view vw_trofeus)
    public TrofeusResponse buscarTrofeus(Long id) {
        buscarPorId(id);

        return jdbcTemplate.queryForObject("""
                        SELECT jogador_id, nickname, nome, ouro, prata, bronze, torneios_disputados
                          FROM vw_trofeus
                         WHERE jogador_id = ?
                        """,
                new DataClassRowMapper<>(TrofeusResponse.class), id);
    }

    // Quem esta sendo inscrito em um torneio/evento: o proprio jogador logado ou,
    // quando a equipe da loja inscreve outra pessoa, o jogadorId informado
    public UsuarioJogador resolverInscrito(Long jogadorId, Long lojaId) {
        Conta logada = permissaoService.contaLogada();

        if (jogadorId == null || jogadorId.equals(logada.getId())) {
            if (logada.getTipo() != TipoConta.JOGADOR) {
                throw new ResponseStatusException(
                        HttpStatus.BAD_REQUEST, "Informe o jogadorId do jogador que será inscrito");
            }
            return buscarPorId(logada.getId());
        }

        permissaoService.verificarEquipeLoja(lojaId);
        return buscarPorId(jogadorId);
    }

    private String maiusculo(String texto) {
        return texto == null ? null : texto.toUpperCase(Locale.ROOT);
    }
}

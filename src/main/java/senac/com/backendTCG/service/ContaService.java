package senac.com.backendTCG.service;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import senac.com.backendTCG.dto.AlterarSenhaRequest;
import senac.com.backendTCG.dto.LoginResponse;
import senac.com.backendTCG.entity.Conta;
import senac.com.backendTCG.entity.enums.TipoConta;
import senac.com.backendTCG.repository.AdministradorRepository;
import senac.com.backendTCG.repository.ContaRepository;
import senac.com.backendTCG.repository.UsuarioJogadorRepository;
import senac.com.backendTCG.repository.UsuarioLojaRepository;
import senac.com.backendTCG.security.JwtUtils;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;

@Service
@RequiredArgsConstructor
public class ContaService {

    private final ContaRepository contaRepository;
    private final UsuarioJogadorRepository usuarioJogadorRepository;
    private final UsuarioLojaRepository usuarioLojaRepository;
    private final AdministradorRepository administradorRepository;
    private final PasswordEncoder passwordEncoder;
    private final PermissaoService permissaoService;
    private final JwtUtils jwtUtils;

    public List<Conta> listarTodos(TipoConta tipo) {
        permissaoService.verificarAdmin();

        return tipo == null
                ? contaRepository.findAll()
                : contaRepository.findByTipo(tipo);
    }

    public Conta buscarPorId(Long id) {
        return contaRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Conta não encontrada"));
    }

    public Conta consultar(Long id) {
        permissaoService.verificarContaOuAdmin(id);
        return buscarPorId(id);
    }

    // Usado pelos cadastros de loja, jogador e administrador
    public Conta criar(String email, String senha, TipoConta tipo) {
        String emailNormalizado = normalizarEmail(email);

        if (contaRepository.existsByEmail(emailNormalizado)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Email já cadastrado");
        }

        Conta conta = new Conta();
        conta.setEmail(emailNormalizado);
        conta.setSenhaHash(passwordEncoder.encode(senha));
        conta.setTipo(tipo);

        return contaRepository.save(conta);
    }

    // Devolve um token novo: os tokens emitidos com a senha antiga deixam de valer
    @Transactional
    public LoginResponse alterarSenha(Long id, AlterarSenhaRequest request) {
        Conta conta = verificarPropriaConta(id);
        verificarSenhaAtual(conta, request.senhaAtual());

        conta.setSenhaHash(passwordEncoder.encode(request.novaSenha()));
        contaRepository.save(conta);

        return respostaDeLogin(conta, jwtUtils.gerarTokenAcesso(conta));
    }

    // Monta a resposta de login com o resumo do perfil (nome, nickname e imagem) de cada tipo de conta
    public LoginResponse respostaDeLogin(Conta conta, String token) {
        return switch (conta.getTipo()) {
            case JOGADOR -> usuarioJogadorRepository.findById(conta.getId())
                    .map(jogador -> LoginResponse.de(conta, token,
                            jogador.getNome(), jogador.getNickname(), jogador.getImagemPerfil()))
                    .orElseGet(() -> LoginResponse.de(conta, token, null, null, null));
            case LOJA -> usuarioLojaRepository.findById(conta.getId())
                    .map(loja -> LoginResponse.de(conta, token, loja.getNome(), null, loja.getImagemPerfil()))
                    .orElseGet(() -> LoginResponse.de(conta, token, null, null, null));
            case ADMIN -> administradorRepository.findById(conta.getId())
                    .map(admin -> LoginResponse.de(conta, token, admin.getNome(), null, null))
                    .orElseGet(() -> LoginResponse.de(conta, token, null, null, null));
        };
    }

    @Transactional
    public Conta alterarStatus(Long id, Boolean ativo) {
        Conta admin = permissaoService.verificarAdmin();

        if (admin.getId().equals(id) && !ativo) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Você não pode desativar a própria conta");
        }

        Conta conta = buscarPorId(id);
        conta.setAtivo(ativo);
        return contaRepository.save(conta);
    }

    // Soft delete (coluna deletado_em): preserva o historico de torneios e bloqueia o login
    @Transactional
    public void deletar(Long id) {
        Conta logada = permissaoService.verificarContaOuAdmin(id);
        Conta conta = buscarPorId(id);

        if (conta.getDeletadoEm() != null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Conta não encontrada");
        }

        if (permissaoService.isAdmin(conta) && logada.getId().equals(id)) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST, "Um administrador não pode excluir a própria conta");
        }

        conta.setAtivo(false);
        conta.setDeletadoEm(LocalDateTime.now());
        contaRepository.save(conta);
    }

    @Transactional
    public Conta registrarLogin(String email) {
        Conta conta = contaRepository.findByEmail(normalizarEmail(email))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Email ou senha inválidos"));

        conta.setUltimoLogin(LocalDateTime.now());
        return contaRepository.save(conta);
    }

    public static String normalizarEmail(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }

    private Conta verificarPropriaConta(Long id) {
        Conta conta = permissaoService.contaLogada();

        if (!conta.getId().equals(id)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Só é possível alterar a própria conta");
        }

        return conta;
    }

    private void verificarSenhaAtual(Conta conta, String senhaAtual) {
        if (!passwordEncoder.matches(senhaAtual, conta.getSenhaHash())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Senha atual incorreta");
        }
    }
}

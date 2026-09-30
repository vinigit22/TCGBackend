package senac.com.backendTCG.service;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import senac.com.backendTCG.dto.AlterarSenhaRequest;
import senac.com.backendTCG.entity.Conta;
import senac.com.backendTCG.entity.enums.TipoConta;
import senac.com.backendTCG.repository.ContaRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;

@Service
@RequiredArgsConstructor
public class ContaService {

    private final ContaRepository contaRepository;
    private final PasswordEncoder passwordEncoder;
    private final PermissaoService permissaoService;

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

    @Transactional
    public Conta alterarSenha(Long id, AlterarSenhaRequest request) {
        Conta conta = permissaoService.contaLogada();

        if (!conta.getId().equals(id)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Só é possível alterar a própria senha");
        }

        if (!passwordEncoder.matches(request.senhaAtual(), conta.getSenhaHash())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Senha atual incorreta");
        }

        conta.setSenhaHash(passwordEncoder.encode(request.novaSenha()));
        return contaRepository.save(conta);
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
}

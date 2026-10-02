package senac.com.backendTCG.service;

import io.jsonwebtoken.Claims;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import senac.com.backendTCG.dto.EsqueciSenhaRequest;
import senac.com.backendTCG.dto.LoginRequest;
import senac.com.backendTCG.dto.LoginResponse;
import senac.com.backendTCG.dto.RedefinirSenhaRequest;
import senac.com.backendTCG.dto.RegistroJogadorRequest;
import senac.com.backendTCG.dto.RegistroLojaRequest;
import senac.com.backendTCG.entity.Conta;
import senac.com.backendTCG.repository.ContaRepository;
import senac.com.backendTCG.security.JwtUtils;
import senac.com.backendTCG.security.JwtUtils.Finalidade;
import senac.com.backendTCG.security.LimiteTentativas;
import senac.com.backendTCG.security.TokensRevogados;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final AuthenticationManager authenticationManager;
    private final JwtUtils jwtUtils;
    private final ContaService contaService;
    private final ContaRepository contaRepository;
    private final UsuarioJogadorService usuarioJogadorService;
    private final UsuarioLojaService usuarioLojaService;
    private final PermissaoService permissaoService;
    private final PasswordEncoder passwordEncoder;
    private final EmailService emailService;
    private final LimiteTentativas limiteTentativas;
    private final TokensRevogados tokensRevogados;

    // Depois de 5 senhas erradas em 15 minutos, o email fica bloqueado (429) ate a janela passar
    public LoginResponse login(LoginRequest request) {
        String email = ContaService.normalizarEmail(request.email());
        String chave = "login:" + email;

        limiteTentativas.verificar(chave);

        try {
            // Lanca BadCredentialsException (401) ou DisabledException (403)
            authenticationManager.authenticate(new UsernamePasswordAuthenticationToken(email, request.senha()));
        } catch (BadCredentialsException e) {
            limiteTentativas.registrar(chave);
            throw e;
        }

        limiteTentativas.limpar(chave);
        Conta conta = contaService.registrarLogin(email);
        return LoginResponse.de(conta, jwtUtils.gerarTokenAcesso(conta));
    }

    public LoginResponse registrarJogador(RegistroJogadorRequest request) {
        Conta conta = usuarioJogadorService.criar(request).getConta();
        return LoginResponse.de(conta, jwtUtils.gerarTokenAcesso(conta));
    }

    public LoginResponse registrarLoja(RegistroLojaRequest request) {
        Conta conta = usuarioLojaService.criar(request).getConta();
        return LoginResponse.de(conta, jwtUtils.gerarTokenAcesso(conta));
    }

    public Conta contaLogada() {
        return permissaoService.contaLogada();
    }

    // Encerra o token desta requisicao (os outros dispositivos continuam logados)
    public void logout(String authorization) {
        if (authorization != null && authorization.startsWith("Bearer ")) {
            jwtUtils.ler(authorization.substring(7), Finalidade.ACESSO)
                    .ifPresent(claims -> tokensRevogados.revogar(claims.getId(), claims.getExpiration()));
        }
    }

    // Responde igual exista ou nao a conta, para nao revelar quais emails estao cadastrados
    public void esqueciSenha(EsqueciSenhaRequest request) {
        String email = ContaService.normalizarEmail(request.email());
        String chave = "redefinir-senha:" + email;

        limiteTentativas.verificar(chave);
        limiteTentativas.registrar(chave);

        contaRepository.findByEmail(email)
                .filter(conta -> conta.getAtivo() && conta.getDeletadoEm() == null)
                .ifPresent(conta -> emailService.enviar(conta.getEmail(), "Redefinição de senha - TCG", """
                        Recebemos um pedido para redefinir a sua senha.
                        Use o código abaixo em POST /auth/redefinir-senha. Ele vale por 30 minutos e uma única vez:

                        %s

                        Se não foi você, ignore este email.""".formatted(jwtUtils.gerarTokenRedefinicaoSenha(conta))));
    }

    @Transactional
    public void redefinirSenha(RedefinirSenhaRequest request) {
        Claims claims = jwtUtils.ler(request.token(), Finalidade.REDEFINIR_SENHA).orElseThrow(this::codigoInvalido);
        Conta conta = contaRepository.findByEmail(claims.getSubject()).orElseThrow(this::codigoInvalido);

        // O codigo foi gerado para a senha atual: depois de usado (senha trocada), deixa de valer
        if (!jwtUtils.senhaConfere(claims, conta.getSenhaHash())) {
            throw codigoInvalido();
        }

        conta.setSenhaHash(passwordEncoder.encode(request.novaSenha()));
        contaRepository.save(conta);
        limiteTentativas.limpar("login:" + conta.getEmail());
    }

    private ResponseStatusException codigoInvalido() {
        return new ResponseStatusException(HttpStatus.BAD_REQUEST, "Código inválido ou expirado");
    }
}

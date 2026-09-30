package senac.com.backendTCG.service;

import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.stereotype.Service;
import senac.com.backendTCG.dto.LoginRequest;
import senac.com.backendTCG.dto.LoginResponse;
import senac.com.backendTCG.dto.RegistroJogadorRequest;
import senac.com.backendTCG.dto.RegistroLojaRequest;
import senac.com.backendTCG.entity.Conta;
import senac.com.backendTCG.security.JwtUtils;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final AuthenticationManager authenticationManager;
    private final JwtUtils jwtUtils;
    private final ContaService contaService;
    private final UsuarioJogadorService usuarioJogadorService;
    private final UsuarioLojaService usuarioLojaService;
    private final PermissaoService permissaoService;

    public LoginResponse login(LoginRequest request) {
        String email = ContaService.normalizarEmail(request.email());

        // Lanca BadCredentialsException (401) ou DisabledException (403)
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(email, request.senha()));

        return gerarToken(contaService.registrarLogin(email));
    }

    public LoginResponse registrarJogador(RegistroJogadorRequest request) {
        return gerarToken(usuarioJogadorService.criar(request).getConta());
    }

    public LoginResponse registrarLoja(RegistroLojaRequest request) {
        return gerarToken(usuarioLojaService.criar(request).getConta());
    }

    public Conta contaLogada() {
        return permissaoService.contaLogada();
    }

    private LoginResponse gerarToken(Conta conta) {
        return new LoginResponse(
                jwtUtils.generateToken(conta.getEmail()),
                conta.getId(),
                conta.getEmail(),
                conta.getTipo());
    }
}

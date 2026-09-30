package senac.com.backendTCG.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import senac.com.backendTCG.dto.LoginRequest;
import senac.com.backendTCG.dto.LoginResponse;
import senac.com.backendTCG.dto.RegistroJogadorRequest;
import senac.com.backendTCG.dto.RegistroLojaRequest;
import senac.com.backendTCG.entity.Conta;
import senac.com.backendTCG.service.AuthService;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        return ResponseEntity.ok(authService.login(request));
    }

    @PostMapping("/registro/jogador")
    public ResponseEntity<LoginResponse> registrarJogador(@Valid @RequestBody RegistroJogadorRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(authService.registrarJogador(request));
    }

    @PostMapping("/registro/loja")
    public ResponseEntity<LoginResponse> registrarLoja(@Valid @RequestBody RegistroLojaRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(authService.registrarLoja(request));
    }

    @GetMapping("/me")
    public ResponseEntity<Conta> contaLogada() {
        return ResponseEntity.ok(authService.contaLogada());
    }
}

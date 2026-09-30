package senac.com.backendTCG.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import senac.com.backendTCG.dto.RegistroJogadorRequest;
import senac.com.backendTCG.dto.TrofeusResponse;
import senac.com.backendTCG.dto.UsuarioJogadorRequest;
import senac.com.backendTCG.entity.UsuarioJogador;
import senac.com.backendTCG.service.UsuarioJogadorService;

import java.util.List;

@RestController
@RequestMapping("/jogadores")
@RequiredArgsConstructor
public class UsuarioJogadorController {

    private final UsuarioJogadorService usuarioJogadorService;

    @GetMapping
    public ResponseEntity<List<UsuarioJogador>> listarTodos() {
        return ResponseEntity.ok(usuarioJogadorService.listarTodos());
    }

    @GetMapping("/{id}")
    public ResponseEntity<UsuarioJogador> buscarPorId(@PathVariable Long id) {
        return ResponseEntity.ok(usuarioJogadorService.buscarPorId(id));
    }

    @GetMapping("/nickname/{nickname}")
    public ResponseEntity<UsuarioJogador> buscarPorNickname(@PathVariable String nickname) {
        return ResponseEntity.ok(usuarioJogadorService.buscarPorNickname(nickname));
    }

    @GetMapping("/{id}/trofeus")
    public ResponseEntity<TrofeusResponse> buscarTrofeus(@PathVariable Long id) {
        return ResponseEntity.ok(usuarioJogadorService.buscarTrofeus(id));
    }

    // Mesmo cadastro de /auth/registro/jogador, mas devolve o jogador criado em vez do token
    @PostMapping
    public ResponseEntity<UsuarioJogador> criar(@Valid @RequestBody RegistroJogadorRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(usuarioJogadorService.criar(request));
    }

    @PutMapping("/{id}")
    public ResponseEntity<UsuarioJogador> atualizar(@PathVariable Long id,
                                                    @Valid @RequestBody UsuarioJogadorRequest request) {
        return ResponseEntity.ok(usuarioJogadorService.atualizar(id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deletar(@PathVariable Long id) {
        usuarioJogadorService.deletar(id);
        return ResponseEntity.noContent().build();
    }
}

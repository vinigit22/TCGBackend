package senac.com.backendTCG.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import senac.com.backendTCG.dto.PerfilJogadorResponse;
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

    // Perfil completo do jogador logado (inclui email e data de nascimento, que nao sao publicos)
    @GetMapping("/me")
    public ResponseEntity<PerfilJogadorResponse> meuPerfil() {
        return ResponseEntity.ok(usuarioJogadorService.meuPerfil());
    }

    @GetMapping("/{id}")
    public ResponseEntity<UsuarioJogador> buscarPorId(@PathVariable Long id) {
        return ResponseEntity.ok(usuarioJogadorService.buscarPorId(id));
    }

    // Foto de perfil: multipart com o campo "arquivo" (JPG, PNG ou WEBP). Grava em imagemPerfil o caminho
    // "/uploads/jogadores/<arquivo>" e apaga a foto anterior.
    @PostMapping(value = "/{id}/imagem", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<UsuarioJogador> enviarImagem(@PathVariable Long id,
                                                       @RequestParam("arquivo") MultipartFile arquivo) {
        return ResponseEntity.ok(usuarioJogadorService.atualizarImagem(id, arquivo));
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

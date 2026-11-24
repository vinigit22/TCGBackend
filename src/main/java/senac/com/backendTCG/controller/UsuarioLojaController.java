package senac.com.backendTCG.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import senac.com.backendTCG.dto.AgendaLojaResponse;
import senac.com.backendTCG.dto.FuncionarioRequest;
import senac.com.backendTCG.dto.RegistroLojaRequest;
import senac.com.backendTCG.dto.UsuarioLojaRequest;
import senac.com.backendTCG.dto.VerificacaoLojaRequest;
import senac.com.backendTCG.entity.LojaMembro;
import senac.com.backendTCG.entity.UsuarioLoja;
import senac.com.backendTCG.service.UsuarioLojaService;

import java.util.List;

@RestController
@RequestMapping("/lojas")
@RequiredArgsConstructor
public class UsuarioLojaController {

    private final UsuarioLojaService usuarioLojaService;

    @GetMapping
    public ResponseEntity<List<UsuarioLoja>> listarTodos() {
        return ResponseEntity.ok(usuarioLojaService.listarTodos());
    }

    @GetMapping("/{id}")
    public ResponseEntity<UsuarioLoja> buscarPorId(@PathVariable Long id) {
        return ResponseEntity.ok(usuarioLojaService.buscarPorId(id));
    }

    @GetMapping("/slug/{slug}")
    public ResponseEntity<UsuarioLoja> buscarPorSlug(@PathVariable String slug) {
        return ResponseEntity.ok(usuarioLojaService.buscarPorSlug(slug));
    }

    @GetMapping("/{id}/agenda")
    public ResponseEntity<List<AgendaLojaResponse>> listarAgenda(@PathVariable Long id) {
        return ResponseEntity.ok(usuarioLojaService.listarAgenda(id));
    }

    // Mesmo cadastro de /auth/registro/loja, mas devolve a loja criada em vez do token
    @PostMapping
    public ResponseEntity<UsuarioLoja> criar(@Valid @RequestBody RegistroLojaRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(usuarioLojaService.criar(request));
    }

    @PutMapping("/{id}")
    public ResponseEntity<UsuarioLoja> atualizar(@PathVariable Long id,
                                                 @Valid @RequestBody UsuarioLojaRequest request) {
        return ResponseEntity.ok(usuarioLojaService.atualizar(id, request));
    }

    @PutMapping("/{id}/verificacao")
    public ResponseEntity<UsuarioLoja> alterarVerificacao(@PathVariable Long id,
                                                          @Valid @RequestBody VerificacaoLojaRequest request) {
        return ResponseEntity.ok(usuarioLojaService.alterarVerificacao(id, request.verificada()));
    }

    // Cria uma conta de funcionário vinculada à loja (tipo FUNCIONARIO, papel ORGANIZADOR ou JUIZ)
    @PostMapping("/{id}/funcionarios")
    public ResponseEntity<LojaMembro> criarFuncionario(@PathVariable Long id,
                                                       @Valid @RequestBody FuncionarioRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(usuarioLojaService.criarFuncionario(id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deletar(@PathVariable Long id) {
        usuarioLojaService.deletar(id);
        return ResponseEntity.noContent().build();
    }
}

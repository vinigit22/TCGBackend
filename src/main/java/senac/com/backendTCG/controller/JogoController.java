package senac.com.backendTCG.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import senac.com.backendTCG.dto.JogoRequest;
import senac.com.backendTCG.entity.Jogo;
import senac.com.backendTCG.service.JogoService;

import java.util.List;

@RestController
@RequestMapping("/jogos")
@RequiredArgsConstructor
public class JogoController {

    private final JogoService jogoService;

    @GetMapping
    public ResponseEntity<List<Jogo>> listarTodos(@RequestParam(required = false) Boolean ativo) {
        return ResponseEntity.ok(jogoService.listarTodos(ativo));
    }

    @GetMapping("/{id}")
    public ResponseEntity<Jogo> buscarPorId(@PathVariable Integer id) {
        return ResponseEntity.ok(jogoService.buscarPorId(id));
    }

    @PostMapping
    public ResponseEntity<Jogo> criar(@Valid @RequestBody JogoRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(jogoService.criar(request));
    }

    @PutMapping("/{id}")
    public ResponseEntity<Jogo> atualizar(@PathVariable Integer id,
                                          @Valid @RequestBody JogoRequest request) {
        return ResponseEntity.ok(jogoService.atualizar(id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deletar(@PathVariable Integer id) {
        jogoService.deletar(id);
        return ResponseEntity.noContent().build();
    }
}

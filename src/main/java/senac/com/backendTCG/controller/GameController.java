package senac.com.backendTCG.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import senac.com.backendTCG.dto.GameRequest;
import senac.com.backendTCG.entity.Game;
import senac.com.backendTCG.service.GameService;

import java.util.List;

@RestController
@RequestMapping("/games")
@RequiredArgsConstructor
public class GameController {

    private final GameService gameService;

    @GetMapping
    public ResponseEntity<List<Game>> listar(@RequestParam(required = false) Long partidaId) {
        return ResponseEntity.ok(gameService.listar(partidaId));
    }

    @GetMapping("/{id}")
    public ResponseEntity<Game> buscarPorId(@PathVariable Long id) {
        return ResponseEntity.ok(gameService.buscarPorId(id));
    }

    @PostMapping
    public ResponseEntity<Game> criar(@Valid @RequestBody GameRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(gameService.criar(request));
    }

    @PutMapping("/{id}")
    public ResponseEntity<Game> atualizar(@PathVariable Long id,
                                          @Valid @RequestBody GameRequest request) {
        return ResponseEntity.ok(gameService.atualizar(id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deletar(@PathVariable Long id) {
        gameService.deletar(id);
        return ResponseEntity.noContent().build();
    }
}

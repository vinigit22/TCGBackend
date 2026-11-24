package senac.com.backendTCG.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import senac.com.backendTCG.dto.TorneioResultadoRequest;
import senac.com.backendTCG.entity.TorneioResultado;
import senac.com.backendTCG.service.TorneioResultadoService;

import java.util.List;

@RestController
@RequestMapping("/torneio-resultados")
@RequiredArgsConstructor
public class TorneioResultadoController {

    private final TorneioResultadoService torneioResultadoService;

    @GetMapping
    public ResponseEntity<List<TorneioResultado>> listar(@RequestParam(required = false) Long torneioId,
                                                         @RequestParam(required = false) Long jogadorId) {
        return ResponseEntity.ok(torneioResultadoService.listar(torneioId, jogadorId));
    }

    @GetMapping("/{id}")
    public ResponseEntity<TorneioResultado> buscarPorId(@PathVariable Long id) {
        return ResponseEntity.ok(torneioResultadoService.buscarPorId(id));
    }

    @PostMapping
    public ResponseEntity<TorneioResultado> criar(@Valid @RequestBody TorneioResultadoRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(torneioResultadoService.criar(request));
    }

    @PutMapping("/{id}")
    public ResponseEntity<TorneioResultado> atualizar(@PathVariable Long id,
                                                      @Valid @RequestBody TorneioResultadoRequest request) {
        return ResponseEntity.ok(torneioResultadoService.atualizar(id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deletar(@PathVariable Long id) {
        torneioResultadoService.deletar(id);
        return ResponseEntity.noContent().build();
    }
}

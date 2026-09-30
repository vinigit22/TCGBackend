package senac.com.backendTCG.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import senac.com.backendTCG.dto.RodadaRequest;
import senac.com.backendTCG.entity.Rodada;
import senac.com.backendTCG.service.RodadaService;

import java.util.List;

@RestController
@RequestMapping("/rodadas")
@RequiredArgsConstructor
public class RodadaController {

    private final RodadaService rodadaService;

    @GetMapping
    public ResponseEntity<List<Rodada>> listar(@RequestParam(required = false) Long torneioId) {
        return ResponseEntity.ok(rodadaService.listar(torneioId));
    }

    @GetMapping("/{id}")
    public ResponseEntity<Rodada> buscarPorId(@PathVariable Long id) {
        return ResponseEntity.ok(rodadaService.buscarPorId(id));
    }

    @PostMapping
    public ResponseEntity<Rodada> criar(@Valid @RequestBody RodadaRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(rodadaService.criar(request));
    }

    @PutMapping("/{id}")
    public ResponseEntity<Rodada> atualizar(@PathVariable Long id,
                                            @Valid @RequestBody RodadaRequest request) {
        return ResponseEntity.ok(rodadaService.atualizar(id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deletar(@PathVariable Long id) {
        rodadaService.deletar(id);
        return ResponseEntity.noContent().build();
    }
}

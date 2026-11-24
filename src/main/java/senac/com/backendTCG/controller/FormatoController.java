package senac.com.backendTCG.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import senac.com.backendTCG.dto.FormatoRequest;
import senac.com.backendTCG.entity.Formato;
import senac.com.backendTCG.service.FormatoService;

import java.util.List;

@RestController
@RequestMapping("/formatos")
@RequiredArgsConstructor
public class FormatoController {

    private final FormatoService formatoService;

    @GetMapping
    public ResponseEntity<List<Formato>> listarTodos(@RequestParam(required = false) Integer jogoId) {
        return ResponseEntity.ok(formatoService.listarTodos(jogoId));
    }

    @GetMapping("/{id}")
    public ResponseEntity<Formato> buscarPorId(@PathVariable Integer id) {
        return ResponseEntity.ok(formatoService.buscarPorId(id));
    }

    @PostMapping
    public ResponseEntity<Formato> criar(@Valid @RequestBody FormatoRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(formatoService.criar(request));
    }

    @PutMapping("/{id}")
    public ResponseEntity<Formato> atualizar(@PathVariable Integer id,
                                             @Valid @RequestBody FormatoRequest request) {
        return ResponseEntity.ok(formatoService.atualizar(id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deletar(@PathVariable Integer id) {
        formatoService.deletar(id);
        return ResponseEntity.noContent().build();
    }
}

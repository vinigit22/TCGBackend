package senac.com.backendTCG.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import senac.com.backendTCG.dto.LojaMembroRequest;
import senac.com.backendTCG.entity.LojaMembro;
import senac.com.backendTCG.service.LojaMembroService;

import java.util.List;

@RestController
@RequestMapping("/loja-membros")
@RequiredArgsConstructor
public class LojaMembroController {

    private final LojaMembroService lojaMembroService;

    @GetMapping
    public ResponseEntity<List<LojaMembro>> listar(@RequestParam(required = false) Long lojaId) {
        return ResponseEntity.ok(lojaMembroService.listar(lojaId));
    }

    @GetMapping("/{id}")
    public ResponseEntity<LojaMembro> buscarPorId(@PathVariable Long id) {
        return ResponseEntity.ok(lojaMembroService.buscarPorId(id));
    }

    @PostMapping
    public ResponseEntity<LojaMembro> criar(@Valid @RequestBody LojaMembroRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(lojaMembroService.criar(request));
    }

    @PutMapping("/{id}")
    public ResponseEntity<LojaMembro> atualizar(@PathVariable Long id,
                                                @Valid @RequestBody LojaMembroRequest request) {
        return ResponseEntity.ok(lojaMembroService.atualizar(id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deletar(@PathVariable Long id) {
        lojaMembroService.deletar(id);
        return ResponseEntity.noContent().build();
    }
}

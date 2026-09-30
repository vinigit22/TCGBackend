package senac.com.backendTCG.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import senac.com.backendTCG.dto.InscricaoRequest;
import senac.com.backendTCG.entity.Inscricao;
import senac.com.backendTCG.entity.enums.StatusInscricao;
import senac.com.backendTCG.service.InscricaoService;

import java.util.List;

@RestController
@RequestMapping("/inscricoes")
@RequiredArgsConstructor
public class InscricaoController {

    private final InscricaoService inscricaoService;

    @GetMapping
    public ResponseEntity<List<Inscricao>> listar(@RequestParam(required = false) Long torneioId,
                                                  @RequestParam(required = false) Long jogadorId,
                                                  @RequestParam(required = false) StatusInscricao status) {
        return ResponseEntity.ok(inscricaoService.listar(torneioId, jogadorId, status));
    }

    @GetMapping("/{id}")
    public ResponseEntity<Inscricao> buscarPorId(@PathVariable Long id) {
        return ResponseEntity.ok(inscricaoService.buscarPorId(id));
    }

    @PostMapping
    public ResponseEntity<Inscricao> inscrever(@Valid @RequestBody InscricaoRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(inscricaoService.inscrever(request));
    }

    @PutMapping("/{id}")
    public ResponseEntity<Inscricao> atualizar(@PathVariable Long id,
                                               @Valid @RequestBody InscricaoRequest request) {
        return ResponseEntity.ok(inscricaoService.atualizar(id, request));
    }

    @PutMapping("/{id}/check-in")
    public ResponseEntity<Inscricao> checkIn(@PathVariable Long id) {
        return ResponseEntity.ok(inscricaoService.checkIn(id));
    }

    @PutMapping("/{id}/cancelar")
    public ResponseEntity<Inscricao> cancelar(@PathVariable Long id) {
        return ResponseEntity.ok(inscricaoService.cancelar(id));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deletar(@PathVariable Long id) {
        inscricaoService.deletar(id);
        return ResponseEntity.noContent().build();
    }
}

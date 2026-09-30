package senac.com.backendTCG.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import senac.com.backendTCG.dto.EventoRequest;
import senac.com.backendTCG.entity.Evento;
import senac.com.backendTCG.entity.enums.StatusEvento;
import senac.com.backendTCG.service.EventoService;

import java.util.List;

@RestController
@RequestMapping("/eventos")
@RequiredArgsConstructor
public class EventoController {

    private final EventoService eventoService;

    @GetMapping
    public ResponseEntity<List<Evento>> listar(@RequestParam(required = false) Long lojaId,
                                               @RequestParam(required = false) StatusEvento status) {
        return ResponseEntity.ok(eventoService.listar(lojaId, status));
    }

    @GetMapping("/{id}")
    public ResponseEntity<Evento> buscarPorId(@PathVariable Long id) {
        return ResponseEntity.ok(eventoService.buscarPorId(id));
    }

    @PostMapping
    public ResponseEntity<Evento> criar(@Valid @RequestBody EventoRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(eventoService.criar(request));
    }

    @PutMapping("/{id}")
    public ResponseEntity<Evento> atualizar(@PathVariable Long id,
                                            @Valid @RequestBody EventoRequest request) {
        return ResponseEntity.ok(eventoService.atualizar(id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deletar(@PathVariable Long id) {
        eventoService.deletar(id);
        return ResponseEntity.noContent().build();
    }
}

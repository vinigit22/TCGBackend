package senac.com.backendTCG.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import senac.com.backendTCG.dto.EventoParticipacaoRequest;
import senac.com.backendTCG.entity.EventoParticipacao;
import senac.com.backendTCG.service.EventoParticipacaoService;

import java.util.List;

@RestController
@RequestMapping("/evento-participacoes")
@RequiredArgsConstructor
public class EventoParticipacaoController {

    private final EventoParticipacaoService eventoParticipacaoService;

    @GetMapping
    public ResponseEntity<List<EventoParticipacao>> listar(@RequestParam(required = false) Long eventoId,
                                                           @RequestParam(required = false) Long jogadorId) {
        return ResponseEntity.ok(eventoParticipacaoService.listar(eventoId, jogadorId));
    }

    @GetMapping("/{id}")
    public ResponseEntity<EventoParticipacao> buscarPorId(@PathVariable Long id) {
        return ResponseEntity.ok(eventoParticipacaoService.buscarPorId(id));
    }

    @PostMapping
    public ResponseEntity<EventoParticipacao> participar(@RequestBody EventoParticipacaoRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(eventoParticipacaoService.participar(request));
    }

    @PutMapping("/{id}")
    public ResponseEntity<EventoParticipacao> atualizar(@PathVariable Long id,
                                                        @RequestBody EventoParticipacaoRequest request) {
        return ResponseEntity.ok(eventoParticipacaoService.atualizar(id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deletar(@PathVariable Long id) {
        eventoParticipacaoService.deletar(id);
        return ResponseEntity.noContent().build();
    }
}

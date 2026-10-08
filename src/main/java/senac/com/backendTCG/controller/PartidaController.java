package senac.com.backendTCG.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import senac.com.backendTCG.dto.DesempateRequest;
import senac.com.backendTCG.dto.PartidaRequest;
import senac.com.backendTCG.dto.ResultadoPartidaRequest;
import senac.com.backendTCG.entity.Partida;
import senac.com.backendTCG.service.PartidaService;

import java.util.List;

@RestController
@RequestMapping("/partidas")
@RequiredArgsConstructor
public class PartidaController {

    private final PartidaService partidaService;

    @GetMapping
    public ResponseEntity<List<Partida>> listar(@RequestParam(required = false) Long rodadaId,
                                                @RequestParam(required = false) Long torneioId) {
        return ResponseEntity.ok(partidaService.listar(rodadaId, torneioId));
    }

    @GetMapping("/{id}")
    public ResponseEntity<Partida> buscarPorId(@PathVariable Long id) {
        return ResponseEntity.ok(partidaService.buscarPorId(id));
    }

    @PostMapping
    public ResponseEntity<Partida> criar(@Valid @RequestBody PartidaRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(partidaService.criar(request));
    }

    @PutMapping("/{id}")
    public ResponseEntity<Partida> atualizar(@PathVariable Long id,
                                             @Valid @RequestBody PartidaRequest request) {
        return ResponseEntity.ok(partidaService.atualizar(id, request));
    }

    // Registra o resultado e avanca o vencedor na chave (a final encerra o torneio)
    @PostMapping("/{id}/resultado")
    public ResponseEntity<Partida> registrarResultado(@PathVariable Long id,
                                                      @Valid @RequestBody ResultadoPartidaRequest request) {
        return ResponseEntity.ok(partidaService.registrarResultado(id, request));
    }

    // Decide uma partida empatada: { "vencedor": "A" | "B" }
    @PostMapping("/{id}/desempate")
    public ResponseEntity<Partida> registrarDesempate(@PathVariable Long id,
                                                      @Valid @RequestBody DesempateRequest request) {
        return ResponseEntity.ok(partidaService.registrarDesempate(id, request));
    }

    // Desfaz o resultado de uma partida finalizada para corrigi-lo (os games voltam a ser editaveis).
    // So vale enquanto a partida seguinte da chave nao comecou.
    @PostMapping("/{id}/reabrir")
    public ResponseEntity<Partida> reabrir(@PathVariable Long id) {
        return ResponseEntity.ok(partidaService.reabrir(id));
    }

    // Loja convoca os jogadores: abre a janela de 5 minutos para check-in e notifica ambos
    @PostMapping("/{id}/convocar")
    public ResponseEntity<Partida> convocar(@PathVariable Long id) {
        return ResponseEntity.ok(partidaService.convocar(id));
    }

    // Jogador confirma presença dentro da janela de 5 minutos (requer autenticacao do proprio jogador)
    @PostMapping("/{id}/check-in")
    public ResponseEntity<Partida> checkIn(@PathVariable Long id) {
        return ResponseEntity.ok(partidaService.checkIn(id));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deletar(@PathVariable Long id) {
        partidaService.deletar(id);
        return ResponseEntity.noContent().build();
    }
}

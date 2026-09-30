package senac.com.backendTCG.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import senac.com.backendTCG.dto.ChaveamentoResponse;
import senac.com.backendTCG.dto.StatusTorneioRequest;
import senac.com.backendTCG.dto.TorneioRequest;
import senac.com.backendTCG.dto.TorneioVagasResponse;
import senac.com.backendTCG.entity.Torneio;
import senac.com.backendTCG.entity.enums.StatusTorneio;
import senac.com.backendTCG.service.TorneioService;

import java.util.List;

@RestController
@RequestMapping("/torneios")
@RequiredArgsConstructor
public class TorneioController {

    private final TorneioService torneioService;

    @GetMapping
    public ResponseEntity<List<Torneio>> listar(@RequestParam(required = false) Long lojaId,
                                                @RequestParam(required = false) Integer jogoId,
                                                @RequestParam(required = false) StatusTorneio status) {
        return ResponseEntity.ok(torneioService.listar(lojaId, jogoId, status));
    }

    @GetMapping("/{id}")
    public ResponseEntity<Torneio> buscarPorId(@PathVariable Long id) {
        return ResponseEntity.ok(torneioService.buscarPorId(id));
    }

    @PostMapping
    public ResponseEntity<Torneio> criar(@Valid @RequestBody TorneioRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(torneioService.criar(request));
    }

    @PutMapping("/{id}")
    public ResponseEntity<Torneio> atualizar(@PathVariable Long id,
                                             @Valid @RequestBody TorneioRequest request) {
        return ResponseEntity.ok(torneioService.atualizar(id, request));
    }

    @PutMapping("/{id}/status")
    public ResponseEntity<Torneio> alterarStatus(@PathVariable Long id,
                                                 @Valid @RequestBody StatusTorneioRequest request) {
        return ResponseEntity.ok(torneioService.alterarStatus(id, request.status()));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deletar(@PathVariable Long id) {
        torneioService.deletar(id);
        return ResponseEntity.noContent().build();
    }

    // Executa a procedure sp_gerar_chaveamento (exige status INSCRICOES_ENCERRADAS)
    @PostMapping("/{id}/chaveamento")
    public ResponseEntity<List<ChaveamentoResponse>> gerarChaveamento(@PathVariable Long id) {
        return ResponseEntity.status(HttpStatus.CREATED).body(torneioService.gerarChaveamento(id));
    }

    @GetMapping("/{id}/chaveamento")
    public ResponseEntity<List<ChaveamentoResponse>> listarChaveamento(@PathVariable Long id) {
        return ResponseEntity.ok(torneioService.listarChaveamento(id));
    }

    @GetMapping("/{id}/vagas")
    public ResponseEntity<TorneioVagasResponse> consultarVagas(@PathVariable Long id) {
        return ResponseEntity.ok(torneioService.consultarVagas(id));
    }
}

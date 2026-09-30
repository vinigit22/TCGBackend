package senac.com.backendTCG.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import senac.com.backendTCG.dto.AlterarSenhaRequest;
import senac.com.backendTCG.dto.StatusContaRequest;
import senac.com.backendTCG.entity.Conta;
import senac.com.backendTCG.entity.enums.TipoConta;
import senac.com.backendTCG.service.ContaService;

import java.util.List;

// As contas sao criadas pelos cadastros de jogador, loja e administrador
@RestController
@RequestMapping("/contas")
@RequiredArgsConstructor
public class ContaController {

    private final ContaService contaService;

    @GetMapping
    public ResponseEntity<List<Conta>> listarTodos(@RequestParam(required = false) TipoConta tipo) {
        return ResponseEntity.ok(contaService.listarTodos(tipo));
    }

    @GetMapping("/{id}")
    public ResponseEntity<Conta> buscarPorId(@PathVariable Long id) {
        return ResponseEntity.ok(contaService.consultar(id));
    }

    @PutMapping("/{id}/senha")
    public ResponseEntity<Conta> alterarSenha(@PathVariable Long id,
                                              @Valid @RequestBody AlterarSenhaRequest request) {
        return ResponseEntity.ok(contaService.alterarSenha(id, request));
    }

    @PutMapping("/{id}/status")
    public ResponseEntity<Conta> alterarStatus(@PathVariable Long id,
                                               @Valid @RequestBody StatusContaRequest request) {
        return ResponseEntity.ok(contaService.alterarStatus(id, request.ativo()));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deletar(@PathVariable Long id) {
        contaService.deletar(id);
        return ResponseEntity.noContent().build();
    }
}

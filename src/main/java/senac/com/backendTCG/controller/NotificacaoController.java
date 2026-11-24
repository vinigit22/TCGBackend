package senac.com.backendTCG.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import senac.com.backendTCG.dto.NotificacaoRequest;
import senac.com.backendTCG.entity.Notificacao;
import senac.com.backendTCG.service.NotificacaoService;

import java.util.List;

// Sempre as notificacoes da conta do token (menos o POST, que e exclusivo de admin)
@RestController
@RequestMapping("/notificacoes")
@RequiredArgsConstructor
public class NotificacaoController {

    private final NotificacaoService notificacaoService;

    @GetMapping
    public ResponseEntity<List<Notificacao>> listarMinhas() {
        return ResponseEntity.ok(notificacaoService.listarMinhas());
    }

    @GetMapping("/nao-lidas")
    public ResponseEntity<List<Notificacao>> listarNaoLidas() {
        return ResponseEntity.ok(notificacaoService.listarNaoLidas());
    }

    @GetMapping("/nao-lidas/total")
    public ResponseEntity<Long> contarNaoLidas() {
        return ResponseEntity.ok(notificacaoService.contarNaoLidas());
    }

    @GetMapping("/{id}")
    public ResponseEntity<Notificacao> buscarPorId(@PathVariable Long id) {
        return ResponseEntity.ok(notificacaoService.buscarPorId(id));
    }

    @PostMapping
    public ResponseEntity<Notificacao> criar(@Valid @RequestBody NotificacaoRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(notificacaoService.criar(request));
    }

    @PutMapping("/{id}/lida")
    public ResponseEntity<Notificacao> marcarComoLida(@PathVariable Long id) {
        return ResponseEntity.ok(notificacaoService.marcarComoLida(id));
    }

    @PutMapping("/lidas")
    public ResponseEntity<Void> marcarTodasComoLidas() {
        notificacaoService.marcarTodasComoLidas();
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deletar(@PathVariable Long id) {
        notificacaoService.deletar(id);
        return ResponseEntity.noContent().build();
    }
}

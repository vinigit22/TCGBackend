package senac.com.backendTCG.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import senac.com.backendTCG.dto.AdministradorRequest;
import senac.com.backendTCG.dto.RegistroAdministradorRequest;
import senac.com.backendTCG.entity.Administrador;
import senac.com.backendTCG.service.AdministradorService;

import java.util.List;

@RestController
@RequestMapping("/administradores")
@RequiredArgsConstructor
public class AdministradorController {

    private final AdministradorService administradorService;

    @GetMapping
    public ResponseEntity<List<Administrador>> listarTodos() {
        return ResponseEntity.ok(administradorService.listarTodos());
    }

    @GetMapping("/{id}")
    public ResponseEntity<Administrador> buscarPorId(@PathVariable Long id) {
        return ResponseEntity.ok(administradorService.buscarPorId(id));
    }

    @PostMapping
    public ResponseEntity<Administrador> criar(@Valid @RequestBody RegistroAdministradorRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(administradorService.criar(request));
    }

    @PutMapping("/{id}")
    public ResponseEntity<Administrador> atualizar(@PathVariable Long id,
                                                   @Valid @RequestBody AdministradorRequest request) {
        return ResponseEntity.ok(administradorService.atualizar(id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deletar(@PathVariable Long id) {
        administradorService.deletar(id);
        return ResponseEntity.noContent().build();
    }
}

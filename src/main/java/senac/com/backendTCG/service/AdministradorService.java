package senac.com.backendTCG.service;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import senac.com.backendTCG.dto.AdministradorRequest;
import senac.com.backendTCG.dto.RegistroAdministradorRequest;
import senac.com.backendTCG.entity.Administrador;
import senac.com.backendTCG.entity.Conta;
import senac.com.backendTCG.entity.enums.TipoConta;
import senac.com.backendTCG.repository.AdministradorRepository;

import java.util.List;

@Service
@RequiredArgsConstructor
public class AdministradorService {

    private final AdministradorRepository administradorRepository;
    private final ContaService contaService;
    private final PermissaoService permissaoService;

    public List<Administrador> listarTodos() {
        permissaoService.verificarAdmin();
        return administradorRepository.findByConta_DeletadoEmIsNullOrderByNomeAsc();
    }

    public Administrador buscarPorId(Long id) {
        permissaoService.verificarAdmin();

        return administradorRepository.findById(id)
                .filter(admin -> admin.getConta().getDeletadoEm() == null)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Administrador não encontrado"));
    }

    // Somente um admin cria outro admin
    @Transactional
    public Administrador criar(RegistroAdministradorRequest request) {
        permissaoService.verificarAdmin();

        Conta conta = contaService.criar(request.email(), request.senha(), TipoConta.ADMIN);
        conta.setEmailVerificado(true);

        Administrador administrador = new Administrador();
        administrador.setConta(conta);
        administrador.setNome(request.nome());

        return administradorRepository.save(administrador);
    }

    @Transactional
    public Administrador atualizar(Long id, AdministradorRequest request) {
        Administrador administrador = buscarPorId(id);
        administrador.setNome(request.nome());
        return administradorRepository.save(administrador);
    }

    @Transactional
    public void deletar(Long id) {
        buscarPorId(id);
        contaService.deletar(id);
    }
}

package senac.com.backendTCG.service;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import senac.com.backendTCG.dto.LojaMembroRequest;
import senac.com.backendTCG.entity.Conta;
import senac.com.backendTCG.entity.LojaMembro;
import senac.com.backendTCG.entity.UsuarioLoja;
import senac.com.backendTCG.repository.LojaMembroRepository;

import java.util.List;

// Equipe da loja (proprietarios, organizadores e juizes)
@Service
@RequiredArgsConstructor
public class LojaMembroService {

    private final LojaMembroRepository lojaMembroRepository;
    private final UsuarioLojaService usuarioLojaService;
    private final ContaService contaService;
    private final PermissaoService permissaoService;

    public List<LojaMembro> listar(Long lojaId) {
        if (lojaId == null) {
            permissaoService.verificarAdmin();
            return lojaMembroRepository.findAll();
        }

        permissaoService.verificarEquipeLoja(lojaId);
        return lojaMembroRepository.findByLoja_ContaId(lojaId);
    }

    public LojaMembro buscarPorId(Long id) {
        LojaMembro membro = lojaMembroRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Membro não encontrado"));

        permissaoService.verificarEquipeLoja(membro.getLoja().getContaId());
        return membro;
    }

    @Transactional
    public LojaMembro criar(LojaMembroRequest request) {
        if (request.lojaId() == null || request.contaId() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Informe lojaId e contaId");
        }

        UsuarioLoja loja = usuarioLojaService.buscarPorId(request.lojaId());
        permissaoService.verificarProprietarioLoja(loja.getContaId());

        Conta conta = contaService.buscarPorId(request.contaId());
        if (conta.getDeletadoEm() != null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Conta não encontrada");
        }

        if (lojaMembroRepository.existsByLoja_ContaIdAndConta_Id(loja.getContaId(), conta.getId())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Esta conta já faz parte da equipe da loja");
        }

        LojaMembro membro = new LojaMembro();
        membro.setLoja(loja);
        membro.setConta(conta);
        membro.setPapel(request.papel());
        if (request.ativo() != null) {
            membro.setAtivo(request.ativo());
        }

        return lojaMembroRepository.save(membro);
    }

    @Transactional
    public LojaMembro atualizar(Long id, LojaMembroRequest request) {
        LojaMembro membro = buscarPorId(id);
        permissaoService.verificarProprietarioLoja(membro.getLoja().getContaId());

        membro.setPapel(request.papel());
        if (request.ativo() != null) {
            membro.setAtivo(request.ativo());
        }

        return lojaMembroRepository.save(membro);
    }

    @Transactional
    public void deletar(Long id) {
        LojaMembro membro = buscarPorId(id);
        permissaoService.verificarProprietarioLoja(membro.getLoja().getContaId());
        lojaMembroRepository.delete(membro);
    }
}

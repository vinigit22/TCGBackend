package senac.com.backendTCG.service;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import senac.com.backendTCG.dto.FormatoRequest;
import senac.com.backendTCG.entity.Formato;
import senac.com.backendTCG.entity.Jogo;
import senac.com.backendTCG.repository.FormatoRepository;
import senac.com.backendTCG.repository.TorneioRepository;

import java.util.List;

@Service
@RequiredArgsConstructor
public class FormatoService {

    private final FormatoRepository formatoRepository;
    private final TorneioRepository torneioRepository;
    private final JogoService jogoService;
    private final PermissaoService permissaoService;

    public List<Formato> listarTodos(Integer jogoId) {
        return jogoId == null
                ? formatoRepository.findAll()
                : formatoRepository.findByJogo_Id(jogoId);
    }

    public Formato buscarPorId(Integer id) {
        return formatoRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Formato não encontrado"));
    }

    @Transactional
    public Formato criar(FormatoRequest request) {
        permissaoService.verificarAdmin();
        Jogo jogo = jogoService.buscarPorId(request.jogoId());

        if (formatoRepository.existsByJogo_IdAndNome(jogo.getId(), request.nome())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Este jogo já possui um formato com esse nome");
        }

        Formato formato = new Formato();
        formato.setJogo(jogo);
        formato.setNome(request.nome());
        if (request.ativo() != null) {
            formato.setAtivo(request.ativo());
        }

        return formatoRepository.save(formato);
    }

    @Transactional
    public Formato atualizar(Integer id, FormatoRequest request) {
        permissaoService.verificarAdmin();
        Formato formato = buscarPorId(id);
        Jogo jogo = jogoService.buscarPorId(request.jogoId());

        if (formatoRepository.existsByJogo_IdAndNomeAndIdNot(jogo.getId(), request.nome(), id)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Este jogo já possui um formato com esse nome");
        }

        formato.setJogo(jogo);
        formato.setNome(request.nome());
        if (request.ativo() != null) {
            formato.setAtivo(request.ativo());
        }

        return formatoRepository.save(formato);
    }

    @Transactional
    public void deletar(Integer id) {
        permissaoService.verificarAdmin();
        Formato formato = buscarPorId(id);

        if (torneioRepository.existsByFormato_Id(id)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Formato possui torneios vinculados. Desative-o (ativo = false) em vez de excluir.");
        }

        formatoRepository.delete(formato);
    }
}

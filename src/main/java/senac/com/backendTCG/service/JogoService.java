package senac.com.backendTCG.service;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import senac.com.backendTCG.dto.JogoRequest;
import senac.com.backendTCG.entity.Jogo;
import senac.com.backendTCG.repository.JogoRepository;
import senac.com.backendTCG.repository.TorneioRepository;
import senac.com.backendTCG.util.SlugUtils;

import java.util.List;

@Service
@RequiredArgsConstructor
public class JogoService {

    private final JogoRepository jogoRepository;
    private final TorneioRepository torneioRepository;
    private final PermissaoService permissaoService;

    public List<Jogo> listarTodos(Boolean ativo) {
        return ativo == null
                ? jogoRepository.findAll()
                : jogoRepository.findByAtivo(ativo);
    }

    public Jogo buscarPorId(Integer id) {
        return jogoRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Jogo não encontrado"));
    }

    @Transactional
    public Jogo criar(JogoRequest request) {
        permissaoService.verificarAdmin();

        String slug = SlugUtils.definir(request.slug(), request.nome(), null);

        if (jogoRepository.existsByNome(request.nome())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Nome já cadastrado");
        }

        if (jogoRepository.existsBySlug(slug)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Slug já cadastrado");
        }

        Jogo jogo = new Jogo();
        jogo.setNome(request.nome());
        jogo.setSlug(slug);
        jogo.setIcone(request.icone());
        if (request.ativo() != null) {
            jogo.setAtivo(request.ativo());
        }

        return jogoRepository.save(jogo);
    }

    @Transactional
    public Jogo atualizar(Integer id, JogoRequest request) {
        permissaoService.verificarAdmin();
        Jogo jogo = buscarPorId(id);

        String slug = SlugUtils.definir(request.slug(), request.nome(), jogo.getSlug());

        if (jogoRepository.existsByNomeAndIdNot(request.nome(), id)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Nome já cadastrado");
        }

        if (jogoRepository.existsBySlugAndIdNot(slug, id)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Slug já cadastrado");
        }

        jogo.setNome(request.nome());
        jogo.setSlug(slug);
        jogo.setIcone(request.icone());
        if (request.ativo() != null) {
            jogo.setAtivo(request.ativo());
        }

        return jogoRepository.save(jogo);
    }

    // Os formatos do jogo sao apagados junto (ON DELETE CASCADE)
    @Transactional
    public void deletar(Integer id) {
        permissaoService.verificarAdmin();
        Jogo jogo = buscarPorId(id);

        if (torneioRepository.existsByJogo_Id(id)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Jogo possui torneios vinculados. Desative-o (ativo = false) em vez de excluir.");
        }

        jogoRepository.delete(jogo);
    }
}

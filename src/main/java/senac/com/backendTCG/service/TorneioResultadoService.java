package senac.com.backendTCG.service;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import senac.com.backendTCG.dto.TorneioResultadoRequest;
import senac.com.backendTCG.entity.Torneio;
import senac.com.backendTCG.entity.TorneioResultado;
import senac.com.backendTCG.entity.UsuarioJogador;
import senac.com.backendTCG.repository.TorneioResultadoRepository;

import java.util.List;

// Classificacao final. Pode ser registrada mesmo com o torneio FINALIZADO.
@Service
@RequiredArgsConstructor
public class TorneioResultadoService {

    private final TorneioResultadoRepository torneioResultadoRepository;
    private final TorneioService torneioService;
    private final UsuarioJogadorService usuarioJogadorService;
    private final PermissaoService permissaoService;

    public List<TorneioResultado> listar(Long torneioId, Long jogadorId) {
        return torneioResultadoRepository.filtrar(torneioId, jogadorId);
    }

    public TorneioResultado buscarPorId(Long id) {
        return torneioResultadoRepository.findById(id)
                .filter(resultado -> resultado.getTorneio().getDeletadoEm() == null)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Resultado não encontrado"));
    }

    @Transactional
    public TorneioResultado criar(TorneioResultadoRequest request) {
        if (request.torneioId() == null || request.jogadorId() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Informe torneioId e jogadorId");
        }

        Torneio torneio = torneioService.buscarPorId(request.torneioId());
        permissaoService.verificarEquipeLoja(torneio.getLoja().getContaId());
        UsuarioJogador jogador = usuarioJogadorService.buscarPorId(request.jogadorId());

        if (torneioResultadoRepository.existsByTorneio_IdAndJogador_ContaId(torneio.getId(), jogador.getContaId())) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT, "Este jogador já tem resultado registrado neste torneio");
        }

        TorneioResultado resultado = new TorneioResultado();
        resultado.setTorneio(torneio);
        resultado.setJogador(jogador);
        preencher(resultado, request);

        return torneioResultadoRepository.save(resultado);
    }

    @Transactional
    public TorneioResultado atualizar(Long id, TorneioResultadoRequest request) {
        TorneioResultado resultado = buscarPorId(id);
        permissaoService.verificarEquipeLoja(resultado.getTorneio().getLoja().getContaId());

        preencher(resultado, request);
        return torneioResultadoRepository.save(resultado);
    }

    @Transactional
    public void deletar(Long id) {
        TorneioResultado resultado = buscarPorId(id);
        permissaoService.verificarEquipeLoja(resultado.getTorneio().getLoja().getContaId());
        torneioResultadoRepository.delete(resultado);
    }

    private void preencher(TorneioResultado resultado, TorneioResultadoRequest request) {
        resultado.setColocacao(request.colocacao());
        resultado.setVitorias(request.vitorias() == null ? 0 : request.vitorias());
        resultado.setDerrotas(request.derrotas() == null ? 0 : request.derrotas());
        resultado.setEmpates(request.empates() == null ? 0 : request.empates());
        resultado.setPremioRecebido(request.premioRecebido());
    }
}

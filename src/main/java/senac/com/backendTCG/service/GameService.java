package senac.com.backendTCG.service;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import senac.com.backendTCG.dto.GameRequest;
import senac.com.backendTCG.entity.Game;
import senac.com.backendTCG.entity.Partida;
import senac.com.backendTCG.repository.GameRepository;

import java.util.List;

@Service
@RequiredArgsConstructor
public class GameService {

    private final GameRepository gameRepository;
    private final PartidaService partidaService;
    private final TorneioService torneioService;

    public List<Game> listar(Long partidaId) {
        return partidaId == null
                ? gameRepository.findAll()
                : gameRepository.findByPartida_IdOrderByNumeroAsc(partidaId);
    }

    public Game buscarPorId(Long id) {
        return gameRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Game não encontrado"));
    }

    @Transactional
    public Game criar(GameRequest request) {
        if (request.partidaId() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Informe o partidaId");
        }

        Partida partida = partidaService.buscarPorId(request.partidaId());
        torneioService.verificarPodeGerenciar(partida.getRodada().getTorneio());

        if (gameRepository.existsByPartida_IdAndNumero(partida.getId(), request.numero())) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT, "O game " + request.numero() + " já foi registrado nesta partida");
        }

        Game game = new Game();
        game.setPartida(partida);
        game.setNumero(request.numero());
        game.setResultado(request.resultado());
        game.setDuracaoMin(request.duracaoMin());

        return gameRepository.save(game);
    }

    @Transactional
    public Game atualizar(Long id, GameRequest request) {
        Game game = buscarPorId(id);
        torneioService.verificarPodeGerenciar(game.getPartida().getRodada().getTorneio());

        if (gameRepository.existsByPartida_IdAndNumeroAndIdNot(game.getPartida().getId(), request.numero(), id)) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT, "O game " + request.numero() + " já foi registrado nesta partida");
        }

        game.setNumero(request.numero());
        game.setResultado(request.resultado());
        game.setDuracaoMin(request.duracaoMin());

        return gameRepository.save(game);
    }

    @Transactional
    public void deletar(Long id) {
        Game game = buscarPorId(id);
        torneioService.verificarPodeGerenciar(game.getPartida().getRodada().getTorneio());
        gameRepository.delete(game);
    }
}

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

// Cada game atualiza o placar (gamesA / gamesB / gamesEmpate) da partida.
// Depois que o resultado da partida e registrado, os games nao mudam mais.
@Service
@RequiredArgsConstructor
public class GameService {

    private final GameRepository gameRepository;
    private final PartidaService partidaService;
    private final TorneioService torneioService;
    private final ChaveamentoService chaveamentoService;

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
        verificarPodeAlterar(partida);

        if (gameRepository.existsByPartida_IdAndNumero(partida.getId(), request.numero())) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT, "O game " + request.numero() + " já foi registrado nesta partida");
        }

        Game game = new Game();
        game.setPartida(partida);
        game.setNumero(request.numero());
        game.setResultado(request.resultado());
        game.setDuracaoMin(request.duracaoMin());
        gameRepository.save(game);

        chaveamentoService.atualizarPlacarPelosGames(partida);
        return game;
    }

    @Transactional
    public Game atualizar(Long id, GameRequest request) {
        Game game = buscarPorId(id);
        verificarPodeAlterar(game.getPartida());

        if (gameRepository.existsByPartida_IdAndNumeroAndIdNot(game.getPartida().getId(), request.numero(), id)) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT, "O game " + request.numero() + " já foi registrado nesta partida");
        }

        game.setNumero(request.numero());
        game.setResultado(request.resultado());
        game.setDuracaoMin(request.duracaoMin());
        gameRepository.save(game);

        chaveamentoService.atualizarPlacarPelosGames(game.getPartida());
        return game;
    }

    @Transactional
    public void deletar(Long id) {
        Game game = buscarPorId(id);
        Partida partida = game.getPartida();
        verificarPodeAlterar(partida);

        gameRepository.delete(game);

        if (!chaveamentoService.atualizarPlacarPelosGames(partida)) {
            // Era o ultimo game: zera o placar
            partida.setGamesA(0);
            partida.setGamesB(0);
            partida.setGamesEmpate(0);
        }
    }

    private void verificarPodeAlterar(Partida partida) {
        torneioService.verificarPodeGerenciar(partida.getRodada().getTorneio());
        chaveamentoService.verificarPartidaEmJogo(partida);
    }
}

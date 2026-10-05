package senac.com.backendTCG.security;

import org.springframework.http.HttpStatus;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

// Bloqueia uma chave (ex.: "login:email") depois de 5 tentativas em 15 minutos.
// Usado contra forca bruta no login e contra excesso de emails de senha/verificacao.
// Fica em memoria: reiniciar a aplicacao zera a contagem. As tentativas vencidas sao descartadas,
// e a chave some quando fica sem tentativas, para o mapa nao crescer com cada email digitado.
@Component
public class LimiteTentativas {

    private static final int MAXIMO = 5;
    private static final Duration JANELA = Duration.ofMinutes(15);

    // Cada fila so e alterada dentro de compute/computeIfPresent, que travam a chave
    private final Map<String, Deque<Instant>> tentativas = new ConcurrentHashMap<>();

    // Lanca 429 (Too Many Requests) se a chave ja atingiu o limite
    public void verificar(String chave) {
        Instant[] liberaEm = new Instant[1];

        tentativas.computeIfPresent(chave, (k, registros) -> {
            descartarVencidas(registros);
            if (registros.size() >= MAXIMO) {
                liberaEm[0] = registros.peekFirst().plus(JANELA);
            }
            return registros.isEmpty() ? null : registros;
        });

        if (liberaEm[0] != null) {
            long minutos = Duration.between(Instant.now(), liberaEm[0]).toMinutes() + 1;
            throw new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS,
                    "Muitas tentativas. Tente novamente em " + minutos + " minuto(s).");
        }
    }

    public void registrar(String chave) {
        tentativas.compute(chave, (k, registros) -> {
            Deque<Instant> fila = registros == null ? new ArrayDeque<>() : registros;
            descartarVencidas(fila);
            fila.addLast(Instant.now());
            return fila;
        });
    }

    public void limpar(String chave) {
        tentativas.remove(chave);
    }

    // Remove as chaves que so tinham tentativas vencidas (emails tentados uma vez e nunca mais)
    @Scheduled(fixedRate = 15, timeUnit = TimeUnit.MINUTES)
    public void descartarChavesVencidas() {
        tentativas.keySet().forEach(chave -> tentativas.computeIfPresent(chave, (k, registros) -> {
            descartarVencidas(registros);
            return registros.isEmpty() ? null : registros;
        }));
    }

    private static void descartarVencidas(Deque<Instant> registros) {
        Instant inicioDaJanela = Instant.now().minus(JANELA);
        while (!registros.isEmpty() && registros.peekFirst().isBefore(inicioDaJanela)) {
            registros.pollFirst();
        }
    }
}

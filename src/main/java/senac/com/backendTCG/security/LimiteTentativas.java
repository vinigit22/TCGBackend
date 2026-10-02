package senac.com.backendTCG.security;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

// Bloqueia uma chave (ex.: "login:email") depois de 5 tentativas em 15 minutos.
// Usado contra forca bruta no login e contra excesso de emails de senha/verificacao.
// Fica em memoria: reiniciar a aplicacao zera a contagem.
@Component
public class LimiteTentativas {

    private static final int MAXIMO = 5;
    private static final Duration JANELA = Duration.ofMinutes(15);

    private final Map<String, Deque<Instant>> tentativas = new ConcurrentHashMap<>();

    // Lanca 429 (Too Many Requests) se a chave ja atingiu o limite
    public void verificar(String chave) {
        Deque<Instant> registros = tentativas.get(chave);
        if (registros == null) {
            return;
        }

        synchronized (registros) {
            Instant inicioDaJanela = Instant.now().minus(JANELA);
            while (!registros.isEmpty() && registros.peekFirst().isBefore(inicioDaJanela)) {
                registros.pollFirst();
            }

            if (registros.size() >= MAXIMO) {
                long minutos = Duration.between(Instant.now(), registros.peekFirst().plus(JANELA)).toMinutes() + 1;
                throw new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS,
                        "Muitas tentativas. Tente novamente em " + minutos + " minuto(s).");
            }
        }
    }

    public void registrar(String chave) {
        Deque<Instant> registros = tentativas.computeIfAbsent(chave, k -> new ArrayDeque<>());
        synchronized (registros) {
            registros.addLast(Instant.now());
        }
    }

    public void limpar(String chave) {
        tentativas.remove(chave);
    }
}

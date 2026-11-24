package senac.com.backendTCG.security;

import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.Date;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

// Tokens encerrados por logout, guardados ate expirarem.
// Ficam em memoria: reiniciar a aplicacao limpa a lista, e com varias instancias cada uma teria a sua
// (para isso seria preciso um armazenamento compartilhado, como Redis ou uma tabela).
@Component
public class TokensRevogados {

    private final Map<String, Instant> revogados = new ConcurrentHashMap<>();

    public void revogar(String tokenId, Date expiracao) {
        Instant agora = Instant.now();
        revogados.values().removeIf(expira -> expira.isBefore(agora));
        revogados.put(tokenId, expiracao.toInstant());
    }

    public boolean estaRevogado(String tokenId) {
        return tokenId != null && revogados.containsKey(tokenId);
    }
}

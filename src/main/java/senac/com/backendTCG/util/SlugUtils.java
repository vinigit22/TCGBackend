package senac.com.backendTCG.util;

import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.text.Normalizer;
import java.util.Locale;

public final class SlugUtils {

    private SlugUtils() {
    }

    // Slug informado tem prioridade. Vazio: na edicao mantem o atual; na criacao (atual = null) gera do nome.
    public static String definir(String informado, String nome, String atual) {
        if (informado == null || informado.isBlank()) {
            if (atual != null) {
                return atual;
            }
            informado = nome;
        }

        String slug = gerar(informado);

        if (slug.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Não foi possível gerar um slug válido");
        }

        return slug;
    }

    // "Pokémon TCG!" -> "pokemon-tcg"
    private static String gerar(String texto) {
        String semAcento = Normalizer.normalize(texto, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "");

        return semAcento.toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9]+", "-")
                .replaceAll("(^-+|-+$)", "");
    }
}

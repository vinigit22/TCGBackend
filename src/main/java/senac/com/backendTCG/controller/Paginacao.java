package senac.com.backendTCG.controller;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

// Paginacao opcional das listagens. Sem "page" a lista vem inteira, como sempre veio (os fronts nao mudam).
// Com "page" (a partir de 0) e "size" (padrao 20, maximo 100), vem so aquela fatia e o header
// X-Total-Count informa o total de registros.
final class Paginacao {

    static final String HEADER_TOTAL = "X-Total-Count";
    private static final int TAMANHO_PADRAO = 20;
    private static final int TAMANHO_MAXIMO = 100;

    private Paginacao() {
    }

    // Null quando o cliente nao pediu paginacao
    static Pageable de(Integer page, Integer size) {
        if (page == null && size == null) {
            return null;
        }
        int pagina = page == null ? 0 : page;
        int tamanho = size == null ? TAMANHO_PADRAO : size;
        if (pagina < 0 || tamanho < 1 || tamanho > TAMANHO_MAXIMO) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Paginação inválida: page >= 0 e size entre 1 e " + TAMANHO_MAXIMO);
        }
        return PageRequest.of(pagina, tamanho);
    }

    static <T> ResponseEntity<List<T>> resposta(Page<T> pagina) {
        return ResponseEntity.ok()
                .header(HEADER_TOTAL, String.valueOf(pagina.getTotalElements()))
                .body(pagina.getContent());
    }

    // Fatia uma lista que ja esta na memoria (listagens que precisam de filtro depois da consulta)
    static <T> ResponseEntity<List<T>> resposta(List<T> itens, Pageable pageable) {
        if (pageable == null) {
            return ResponseEntity.ok(itens);
        }
        int inicio = (int) Math.min(pageable.getOffset(), itens.size());
        int fim = Math.min(inicio + pageable.getPageSize(), itens.size());
        return ResponseEntity.ok()
                .header(HEADER_TOTAL, String.valueOf(itens.size()))
                .body(itens.subList(inicio, fim));
    }
}

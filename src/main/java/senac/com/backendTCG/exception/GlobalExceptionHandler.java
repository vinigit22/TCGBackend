package senac.com.backendTCG.exception;

import org.springframework.dao.DataAccessException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.transaction.TransactionSystemException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

import java.sql.SQLException;
import java.util.LinkedHashMap;
import java.util.Map;

// Todas as respostas de erro saem no formato ProblemDetail (RFC 9457):
// { "status": 404, "title": "Not Found", "detail": "Torneio não encontrado", ... }
// ResponseStatusException (usada nos services) ja e tratada pela classe base.
@RestControllerAdvice
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {

    // SQLSTATE usado pelos SIGNAL das triggers do TorneioTCG_SQL.sql (MySQL)
    private static final String SQLSTATE_REGRA_DO_BANCO = "45000";

    // Erros do @Valid: devolve a lista de campos invalidos
    @Override
    protected ResponseEntity<Object> handleMethodArgumentNotValid(MethodArgumentNotValidException ex,
                                                                  HttpHeaders headers,
                                                                  HttpStatusCode status,
                                                                  WebRequest request) {
        Map<String, String> erros = new LinkedHashMap<>();
        ex.getBindingResult().getFieldErrors()
                .forEach(erro -> erros.putIfAbsent(erro.getField(), erro.getDefaultMessage()));

        ProblemDetail problema = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, "Dados inválidos");
        problema.setProperty("erros", erros);
        return ResponseEntity.badRequest().body(problema);
    }

    @ExceptionHandler(BadCredentialsException.class)
    public ProblemDetail handleCredenciaisInvalidas(BadCredentialsException ex) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.UNAUTHORIZED, "Email ou senha inválidos");
    }

    @ExceptionHandler(DisabledException.class)
    public ProblemDetail handleContaDesativada(DisabledException ex) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.FORBIDDEN, "Conta desativada");
    }

    @ExceptionHandler(AuthenticationException.class)
    public ProblemDetail handleAutenticacao(AuthenticationException ex) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.UNAUTHORIZED, "Falha na autenticação");
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ProblemDetail handleAcessoNegado(AccessDeniedException ex) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.FORBIDDEN, "Acesso negado");
    }

    @ExceptionHandler({DataAccessException.class, TransactionSystemException.class})
    public ProblemDetail handleBancoDeDados(RuntimeException ex) {
        SQLException regraDoBanco = encontrarRegraDoBanco(ex);

        // Regra de negocio barrada por trigger: devolve a mensagem do proprio banco
        if (regraDoBanco != null) {
            return ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, regraDoBanco.getMessage());
        }

        if (ex instanceof DataIntegrityViolationException) {
            return ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT,
                    "Operação viola uma restrição do banco (registro duplicado ou vinculado a outros dados)");
        }

        logger.error("Erro ao acessar o banco de dados", ex);
        return ProblemDetail.forStatusAndDetail(HttpStatus.INTERNAL_SERVER_ERROR, "Erro ao acessar o banco de dados");
    }

    // Procura em toda a cadeia de causas: o erro do banco costuma vir embrulhado em outras excecoes
    private SQLException encontrarRegraDoBanco(Throwable ex) {
        for (Throwable causa = ex; causa != null; causa = causa.getCause()) {
            if (causa instanceof SQLException sqlException
                    && SQLSTATE_REGRA_DO_BANCO.equals(sqlException.getSQLState())) {
                return sqlException;
            }
        }
        return null;
    }
}

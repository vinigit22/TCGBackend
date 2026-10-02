package senac.com.backendTCG.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import senac.com.backendTCG.entity.Conta;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.util.Date;
import java.util.HexFormat;
import java.util.Optional;
import java.util.UUID;

@Component
public class JwtUtils {

    // Um token de redefinicao de senha nao serve para login, e vice-versa
    public enum Finalidade { ACESSO, REDEFINIR_SENHA }

    private static final String CLAIM_FINALIDADE = "finalidade";
    private static final String CLAIM_SENHA = "senha";
    private static final Duration VALIDADE_REDEFINICAO = Duration.ofMinutes(30);

    private final SecretKey key;
    private final Duration validadeAcesso;

    public JwtUtils(@Value("${app.jwt.secret}") String secret,
                    @Value("${app.jwt.expiracao-ms}") long expiracaoMs) {
        if (secret == null || secret.getBytes(StandardCharsets.UTF_8).length < 32) {
            throw new IllegalStateException(
                    "Defina a variável de ambiente JWT_SECRET com pelo menos 32 caracteres.");
        }
        this.key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.validadeAcesso = Duration.ofMillis(expiracaoMs);
    }

    // Token de login. Leva a impressao digital da senha: trocar a senha invalida os tokens antigos.
    public String gerarTokenAcesso(Conta conta) {
        return gerar(conta, Finalidade.ACESSO, validadeAcesso);
    }

    // Vale 30 minutos e uma vez so: depois que a senha muda, a impressao digital nao confere mais
    public String gerarTokenRedefinicaoSenha(Conta conta) {
        return gerar(conta, Finalidade.REDEFINIR_SENHA, VALIDADE_REDEFINICAO);
    }

    // Confere assinatura, validade e finalidade. Token invalido = vazio.
    public Optional<Claims> ler(String token, Finalidade finalidade) {
        try {
            Claims claims = Jwts.parser()
                    .verifyWith(key)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();

            return finalidade.name().equals(claims.get(CLAIM_FINALIDADE, String.class))
                    ? Optional.of(claims)
                    : Optional.empty();
        } catch (Exception e) {
            return Optional.empty();
        }
    }

    // O token foi emitido com a senha atual da conta?
    public boolean senhaConfere(Claims claims, String senhaHash) {
        return impressaoDigital(senhaHash).equals(claims.get(CLAIM_SENHA, String.class));
    }

    private String gerar(Conta conta, Finalidade finalidade, Duration validade) {
        Date agora = new Date();

        return Jwts.builder()
                .id(UUID.randomUUID().toString())
                .subject(conta.getEmail())
                .claim(CLAIM_FINALIDADE, finalidade.name())
                .claim(CLAIM_SENHA, impressaoDigital(conta.getSenhaHash()))
                .issuedAt(agora)
                .expiration(new Date(agora.getTime() + validade.toMillis()))
                .signWith(key)
                .compact();
    }

    // Trecho do SHA-256 do hash da senha: nao revela a senha, mas muda sempre que ela muda
    private static String impressaoDigital(String senhaHash) {
        try {
            byte[] hash = MessageDigest.getInstance("SHA-256").digest(senhaHash.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash, 0, 8);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }
}

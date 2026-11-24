package senac.com.backendTCG.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Map;
import java.util.UUID;

// Guarda as imagens enviadas (foto de perfil) no disco do servidor, na pasta app.upload.diretorio.
// Elas ficam acessiveis em GET /uploads/<pasta>/<arquivo> (UploadConfig); no banco vai so esse caminho
// relativo, e o front completa com o endereco da API.
@Slf4j
@Service
public class ArmazenamentoImagemService {

    public static final String PREFIXO_PUBLICO = "/uploads/";

    // Tipo informado pelo app -> extensao gravada. O conteudo tambem e conferido (assinatura do arquivo).
    private static final Map<String, String> EXTENSOES = Map.of(
            "image/jpeg", "jpg",
            "image/png", "png",
            "image/webp", "webp");

    private final Path diretorio;

    public ArmazenamentoImagemService(@Value("${app.upload.diretorio}") String diretorio) {
        this.diretorio = Paths.get(diretorio).toAbsolutePath().normalize();
    }

    public Path getDiretorio() {
        return diretorio;
    }

    // Salva a imagem em <diretorio>/<pasta>/<uuid>.<ext> e devolve o caminho publico ("/uploads/<pasta>/...")
    public String salvar(MultipartFile arquivo, String pasta) {
        if (arquivo == null || arquivo.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Envie um arquivo de imagem");
        }

        String extensao = EXTENSOES.get(arquivo.getContentType());
        if (extensao == null || !conteudoConfere(arquivo, extensao)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Envie uma imagem JPG, PNG ou WEBP");
        }

        String nome = UUID.randomUUID() + "." + extensao;
        Path destino = diretorio.resolve(pasta).resolve(nome);

        try {
            Files.createDirectories(destino.getParent());
            try (InputStream entrada = arquivo.getInputStream()) {
                Files.copy(entrada, destino);
            }
        } catch (IOException e) {
            log.error("Falha ao gravar a imagem em {}", destino, e);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Não foi possível salvar a imagem");
        }

        return PREFIXO_PUBLICO + pasta + "/" + nome;
    }

    // Apaga uma imagem enviada antes. Ignora URLs externas e caminhos fora da pasta de uploads.
    public void removerSeLocal(String caminhoPublico) {
        if (caminhoPublico == null || !caminhoPublico.startsWith(PREFIXO_PUBLICO)) {
            return;
        }

        Path arquivo = diretorio.resolve(caminhoPublico.substring(PREFIXO_PUBLICO.length())).normalize();
        if (!arquivo.startsWith(diretorio)) {
            return;
        }

        try {
            Files.deleteIfExists(arquivo);
        } catch (IOException e) {
            // A imagem antiga sobrando no disco nao impede a troca
            log.warn("Não foi possível apagar a imagem antiga {}", arquivo, e);
        }
    }

    // Confere os primeiros bytes: impede que um arquivo qualquer seja gravado com extensao de imagem
    private static boolean conteudoConfere(MultipartFile arquivo, String extensao) {
        byte[] inicio = new byte[12];
        int lidos;
        try (InputStream entrada = arquivo.getInputStream()) {
            lidos = entrada.readNBytes(inicio, 0, inicio.length);
        } catch (IOException e) {
            return false;
        }
        if (lidos < inicio.length) {
            return false;
        }

        return switch (extensao) {
            case "jpg" -> (inicio[0] & 0xFF) == 0xFF && (inicio[1] & 0xFF) == 0xD8 && (inicio[2] & 0xFF) == 0xFF;
            case "png" -> Arrays.equals(Arrays.copyOfRange(inicio, 0, 8),
                    new byte[]{(byte) 0x89, 'P', 'N', 'G', '\r', '\n', 0x1A, '\n'});
            case "webp" -> new String(inicio, 0, 4).equals("RIFF") && new String(inicio, 8, 4).equals("WEBP");
            default -> false;
        };
    }
}

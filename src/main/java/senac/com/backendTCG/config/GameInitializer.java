package senac.com.backendTCG.config;

import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import senac.com.backendTCG.entity.Formato;
import senac.com.backendTCG.entity.Jogo;
import senac.com.backendTCG.repository.FormatoRepository;
import senac.com.backendTCG.repository.JogoRepository;

// Popula jogos e formatos caso o banco tenha sido criado sem os dados iniciais do script SQL
@Component
@RequiredArgsConstructor
public class GameInitializer implements CommandLineRunner {

    private final JogoRepository jogoRepository;
    private final FormatoRepository formatoRepository;

    @Override
    @Transactional
    public void run(String... args) {
        if (jogoRepository.count() > 0) {
            System.out.println("Jogos já existentes no banco. Nenhum jogo foi inserido.");
            return;
        }

        // Mesmos dados da secao "DADOS INICIAIS DE TESTE" do TorneioTCG_SQL.sql
        criarJogo("Magic: The Gathering", "magic", "Standard", "Modern", "Commander", "Pauper");
        criarJogo("Pokemon TCG", "pokemon", "Standard", "Expandido");
        criarJogo("Yu-Gi-Oh!", "yugioh", "Advanced");
        criarJogo("One Piece Card Game", "one-piece", "Standard");
        criarJogo("Digimon Card Game", "digimon");

        System.out.println("5 jogos inseridos com sucesso!");
    }

    private void criarJogo(String nome, String slug, String... formatos) {
        Jogo jogo = new Jogo();
        jogo.setNome(nome);
        jogo.setSlug(slug);
        jogoRepository.save(jogo);

        for (String nomeFormato : formatos) {
            Formato formato = new Formato();
            formato.setJogo(jogo);
            formato.setNome(nomeFormato);
            formatoRepository.save(formato);
        }
    }
}

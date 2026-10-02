package senac.com.backendTCG.config;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import senac.com.backendTCG.entity.Administrador;
import senac.com.backendTCG.entity.Conta;
import senac.com.backendTCG.entity.enums.TipoConta;
import senac.com.backendTCG.repository.AdministradorRepository;
import senac.com.backendTCG.repository.ContaRepository;
import senac.com.backendTCG.service.ContaService;

// Garante um administrador com senha conhecida (os hashes do script SQL sao apenas exemplos)
@Component
@RequiredArgsConstructor
public class AdminInitializer implements CommandLineRunner {

    private final ContaRepository contaRepository;
    private final AdministradorRepository administradorRepository;
    private final PasswordEncoder passwordEncoder;

    @Value("${app.admin.email}")
    private String email;

    @Value("${app.admin.senha}")
    private String senha;

    @Value("${app.admin.nome}")
    private String nome;

    @Override
    @Transactional
    public void run(String... args) {
        // Mesma normalizacao do login (minusculas), senao ADMIN_EMAIL com maiusculas nunca logaria
        String emailAdmin = ContaService.normalizarEmail(email);

        if (contaRepository.existsByEmail(emailAdmin)) {
            return;
        }

        // Sem senha padrao fora do perfil h2: o admin so e criado com ADMIN_SENHA definida
        if (senha == null || senha.isBlank()) {
            System.out.println("AVISO: administrador não criado. Defina a variável de ambiente ADMIN_SENHA.");
            return;
        }

        Conta conta = new Conta();
        conta.setEmail(emailAdmin);
        conta.setSenhaHash(passwordEncoder.encode(senha));
        conta.setTipo(TipoConta.ADMIN);
        conta.setEmailVerificado(true);
        contaRepository.save(conta);

        Administrador admin = new Administrador();
        admin.setConta(conta);
        admin.setNome(nome);
        administradorRepository.save(admin);

        System.out.println("ADMIN CRIADO: " + emailAdmin);
    }
}

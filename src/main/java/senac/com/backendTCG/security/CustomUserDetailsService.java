package senac.com.backendTCG.security;

import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import senac.com.backendTCG.entity.Conta;
import senac.com.backendTCG.repository.ContaRepository;

@Service
@RequiredArgsConstructor
public class CustomUserDetailsService implements UserDetailsService {

    private final ContaRepository contaRepository;

    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {

        Conta conta = contaRepository.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException("Usuário não encontrado"));

        // ROLE_LOJA, ROLE_JOGADOR ou ROLE_ADMIN
        return User.withUsername(conta.getEmail())
                .password(conta.getSenhaHash())
                .roles(conta.getTipo().name())
                .disabled(!conta.getAtivo() || conta.getDeletadoEm() != null)
                .build();
    }
}

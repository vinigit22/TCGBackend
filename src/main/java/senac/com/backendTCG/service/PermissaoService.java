package senac.com.backendTCG.service;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import senac.com.backendTCG.entity.Conta;
import senac.com.backendTCG.entity.enums.PapelMembro;
import senac.com.backendTCG.entity.enums.TipoConta;
import senac.com.backendTCG.repository.ContaRepository;
import senac.com.backendTCG.repository.LojaMembroRepository;

// Centraliza as regras de "quem pode fazer o que" usadas pelos outros services
@Service
@RequiredArgsConstructor
public class PermissaoService {

    private final ContaRepository contaRepository;
    private final LojaMembroRepository lojaMembroRepository;

    // Conta dona do token JWT da requisicao
    public Conta contaLogada() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();

        if (auth == null || !auth.isAuthenticated() || auth instanceof AnonymousAuthenticationToken) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Faça login para continuar");
        }

        return contaRepository.findByEmail(auth.getName())
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.UNAUTHORIZED, "Faça login para continuar"));
    }

    public boolean isAdmin(Conta conta) {
        return conta.getTipo() == TipoConta.ADMIN;
    }

    public Conta verificarAdmin() {
        Conta conta = contaLogada();

        if (!isAdmin(conta)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Acesso restrito a administradores");
        }

        return conta;
    }

    // A propria conta ou um administrador
    public Conta verificarContaOuAdmin(Long contaId) {
        Conta conta = contaLogada();

        if (!isAdmin(conta) && !conta.getId().equals(contaId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Acesso negado");
        }

        return conta;
    }

    // Equipe da loja: a conta da propria loja, qualquer membro ativo
    // (proprietario, organizador ou juiz) ou um administrador
    public Conta verificarEquipeLoja(Long lojaId) {
        Conta conta = contaLogada();

        if (isAdmin(conta)
                || conta.getId().equals(lojaId)
                || lojaMembroRepository.existsByLoja_ContaIdAndConta_IdAndAtivoTrue(lojaId, conta.getId())) {
            return conta;
        }

        throw new ResponseStatusException(
                HttpStatus.FORBIDDEN, "Apenas a equipe da loja pode realizar esta ação");
    }

    // Dono da loja: a conta da propria loja, membro PROPRIETARIO ativo ou um administrador
    public Conta verificarProprietarioLoja(Long lojaId) {
        Conta conta = contaLogada();

        if (isAdmin(conta)
                || conta.getId().equals(lojaId)
                || lojaMembroRepository.existsByLoja_ContaIdAndConta_IdAndPapelAndAtivoTrue(
                        lojaId, conta.getId(), PapelMembro.PROPRIETARIO)) {
            return conta;
        }

        throw new ResponseStatusException(
                HttpStatus.FORBIDDEN, "Apenas o proprietário da loja pode realizar esta ação");
    }

    // Enderecos nao tem dono no banco; so lojas, equipes de loja e admins podem mexer neles
    public Conta verificarPodeGerenciarEnderecos() {
        Conta conta = contaLogada();

        if (isAdmin(conta)
                || conta.getTipo() == TipoConta.LOJA
                || lojaMembroRepository.existsByConta_IdAndAtivoTrue(conta.getId())) {
            return conta;
        }

        throw new ResponseStatusException(
                HttpStatus.FORBIDDEN, "Apenas lojas e administradores podem gerenciar endereços");
    }
}

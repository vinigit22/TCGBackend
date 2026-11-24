package senac.com.backendTCG.service;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import senac.com.backendTCG.dto.EnderecoRequest;
import senac.com.backendTCG.entity.Endereco;
import senac.com.backendTCG.repository.EnderecoRepository;

import java.util.List;
import java.util.Locale;

@Service
@RequiredArgsConstructor
public class EnderecoService {

    private final EnderecoRepository enderecoRepository;
    private final PermissaoService permissaoService;

    public List<Endereco> listarTodos() {
        return enderecoRepository.findAll();
    }

    public Endereco buscarPorId(Long id) {
        return enderecoRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Endereço não encontrado"));
    }

    @Transactional
    public Endereco criar(EnderecoRequest request) {
        permissaoService.verificarPodeGerenciarEnderecos();

        Endereco endereco = new Endereco();
        preencher(endereco, request);
        return enderecoRepository.save(endereco);
    }

    @Transactional
    public Endereco atualizar(Long id, EnderecoRequest request) {
        permissaoService.verificarPodeGerenciarEnderecos();

        Endereco endereco = buscarPorId(id);
        preencher(endereco, request);
        return enderecoRepository.save(endereco);
    }

    // Lojas, eventos e torneios que usavam o endereco ficam com endereco_id = NULL (ON DELETE SET NULL)
    @Transactional
    public void deletar(Long id) {
        permissaoService.verificarPodeGerenciarEnderecos();
        enderecoRepository.delete(buscarPorId(id));
    }

    private void preencher(Endereco endereco, EnderecoRequest request) {
        endereco.setCep(request.cep());
        endereco.setLogradouro(request.logradouro());
        endereco.setNumero(request.numero());
        endereco.setComplemento(request.complemento());
        endereco.setBairro(request.bairro());
        endereco.setCidade(request.cidade());
        endereco.setEstado(request.estado().toUpperCase(Locale.ROOT));
        endereco.setLatitude(request.latitude());
        endereco.setLongitude(request.longitude());
        endereco.setReferencia(request.referencia());
    }
}

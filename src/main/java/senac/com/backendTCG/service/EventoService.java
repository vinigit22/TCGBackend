package senac.com.backendTCG.service;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import senac.com.backendTCG.dto.EventoRequest;
import senac.com.backendTCG.entity.Evento;
import senac.com.backendTCG.entity.UsuarioLoja;
import senac.com.backendTCG.entity.enums.StatusEvento;
import senac.com.backendTCG.repository.EventoRepository;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class EventoService {

    private final EventoRepository eventoRepository;
    private final UsuarioLojaService usuarioLojaService;
    private final EnderecoService enderecoService;
    private final PermissaoService permissaoService;
    private final NotificacaoService notificacaoService;

    public List<Evento> listar(Long lojaId, StatusEvento status) {
        return eventoRepository.filtrar(lojaId, status);
    }

    public Evento buscarPorId(Long id) {
        return eventoRepository.findByIdAndDeletadoEmIsNull(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Evento não encontrado"));
    }

    @Transactional
    public Evento criar(EventoRequest request) {
        if (request.lojaId() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Informe o lojaId");
        }

        UsuarioLoja loja = usuarioLojaService.buscarPorId(request.lojaId());
        permissaoService.verificarEquipeLoja(loja.getContaId());

        Evento evento = new Evento();
        evento.setLoja(loja);
        preencher(evento, request);

        return eventoRepository.save(evento);
    }

    @Transactional
    public Evento atualizar(Long id, EventoRequest request) {
        Evento evento = buscarPorId(id);
        permissaoService.verificarEquipeLoja(evento.getLoja().getContaId());

        preencher(evento, request);
        Evento salvo = eventoRepository.save(evento);

        notificacaoService.notificarParticipantesEvento(salvo,
                "O evento '" + salvo.getTitulo() + "' foi atualizado. Confira os detalhes.");

        return salvo;
    }

    // Soft delete (coluna deletado_em)
    @Transactional
    public void deletar(Long id) {
        Evento evento = buscarPorId(id);
        permissaoService.verificarEquipeLoja(evento.getLoja().getContaId());

        evento.setDeletadoEm(LocalDateTime.now());
        eventoRepository.save(evento);
    }

    private void preencher(Evento evento, EventoRequest request) {
        // Mesma regra do CHECK ck_evento_datas
        if (request.dataFim() != null && request.dataFim().isBefore(request.dataInicio())) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST, "A data de fim não pode ser anterior à data de início");
        }

        evento.setTitulo(request.titulo());
        evento.setDescricao(request.descricao());
        evento.setImagem(request.imagem());
        if (request.tipo() != null) {
            evento.setTipo(request.tipo());
        }
        evento.setVagasMax(request.vagasMax());
        evento.setEndereco(request.enderecoId() == null ? null : enderecoService.buscarPorId(request.enderecoId()));
        evento.setDataInicio(request.dataInicio());
        evento.setDataFim(request.dataFim());
        if (request.status() != null) {
            evento.setStatus(request.status());
        }
    }
}

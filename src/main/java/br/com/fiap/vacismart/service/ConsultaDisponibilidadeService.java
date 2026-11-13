package br.com.fiap.vacismart.service;

import br.com.fiap.vacismart.domain.enums.SituacaoDisponibilidade;
import br.com.fiap.vacismart.repository.DisponibilidadeConsultaRepository;
import br.com.fiap.vacismart.security.AcessoService;
import br.com.fiap.vacismart.web.dto.*;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.UUID;

@Service
@Transactional(readOnly = true)
public class ConsultaDisponibilidadeService {
    private static final String MENSAGEM = "Disponibilidade informada pela unidade. Consulte as condições de atendimento; não há reserva ou garantia de dose.";
    private final AcessoService acesso;
    private final DisponibilidadeConsultaRepository consultas;

    public ConsultaDisponibilidadeService(AcessoService acesso, DisponibilidadeConsultaRepository consultas) {
        this.acesso = acesso; this.consultas = consultas;
    }

    public PaginaDto<DisponibilidadeSaidaDto> consultar(UUID unidadeId, UUID vacinaId, Pageable pagina) {
        acesso.atual();
        // Validar antes do proxy @Repository, que traduz IllegalArgumentException em erro de acesso a dados.
        for (Sort.Order ordem : pagina.getSort()) {
            if (!java.util.Set.of("unidadeNome", "vacinaNome").contains(ordem.getProperty()))
                throw new IllegalArgumentException("Campo de ordenação não permitido");
        }
        var limitada = PageRequest.of(pagina.getPageNumber(), Math.min(pagina.getPageSize(), 100), pagina.getSort());
        return PaginaDto.de(consultas.consultar(unidadeId, vacinaId, limitada).map(r -> {
            var situacao = r.confirmada() ? SituacaoDisponibilidade.CONFIRMADA
                    : r.retirada() && !r.algumaConfirmada() ? SituacaoDisponibilidade.CONFIRMACAO_RETIRADA
                    : SituacaoDisponibilidade.SEM_INFORMACAO;
            return new DisponibilidadeSaidaDto(
                    new DisponibilidadeSaidaDto.ReferenciaPublica(r.vacinaId(), r.vacinaNome()),
                    new DisponibilidadeSaidaDto.ReferenciaPublica(r.unidadeId(), r.unidadeNome()),
                    situacao, r.atualizadoEm(), MENSAGEM);
        }));
    }
}

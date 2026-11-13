package br.com.fiap.vacismart.repository;

import jakarta.persistence.EntityManager;
import org.hibernate.query.NativeQuery;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Repository;
import java.time.OffsetDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Repository
public class DisponibilidadeConsultaRepository {
    private static final Map<String, String> ORDENACAO = Map.of(
            "unidadeNome", "unidade_nome", "vacinaNome", "vacina_nome");
    private final EntityManager em;

    public DisponibilidadeConsultaRepository(EntityManager em) { this.em = em; }

    public record Registro(UUID unidadeId, String unidadeNome, UUID vacinaId, String vacinaNome,
            boolean confirmada, boolean algumaConfirmada, boolean retirada, OffsetDateTime atualizadoEm) { }

    public Page<Registro> consultar(UUID unidadeId, UUID vacinaId, Pageable pagina) {
        // Somente fragmentos constantes entram no SQL. Valores externos são parâmetros vinculados.
        String filtros = (unidadeId == null ? "" : " and u.id = :unidadeId")
                + (vacinaId == null ? "" : " and v.id = :vacinaId");
        String ordemPagina = ordenar(pagina, "");
        String ordemFinal = ordenar(pagina, "p.");
        String sql = """
                with pares_pagina as (
                    select u.id unidade_id, u.nome unidade_nome, v.id vacina_id, v.nome vacina_nome
                    from unidade_saude u cross join vacina v
                    where 1 = 1
                """ + filtros + " order by " + ordemPagina + """
                    offset :deslocamento rows fetch first :limite rows only
                ), estado_atual as (
                    select p.unidade_id, p.vacina_id,
                        max(case when l.estado = 'LIBERADO' and l.disponibilidade = 'CONFIRMADA'
                            and t.estado = 'RECEBIDO' then 1 else 0 end) confirmada,
                        max(case when l.disponibilidade = 'CONFIRMADA' then 1 else 0 end) alguma_confirmada
                    from pares_pagina p
                    join transporte t on t.unidade_saude_id = p.unidade_id
                    join lote l on l.transporte_id = t.id and l.vacina_id = p.vacina_id
                    group by p.unidade_id, p.vacina_id
                ), historico as (
                    select p.unidade_id, p.vacina_id,
                        max(case when e.acao = 'DISPONIBILIDADE_RETIRADA' then 1 else 0 end) retirada,
                        max(e.data_hora) atualizado_em
                    from pares_pagina p
                    join transporte t on t.unidade_saude_id = p.unidade_id
                    join lote l on l.transporte_id = t.id and l.vacina_id = p.vacina_id
                    join evento_auditoria e on e.referencia_id = l.id and e.entidade = 'Lote'
                        and e.acao in ('DISPONIBILIDADE_CONFIRMADA', 'DISPONIBILIDADE_RETIRADA')
                    group by p.unidade_id, p.vacina_id
                )
                select p.unidade_id, p.unidade_nome, p.vacina_id, p.vacina_nome,
                    coalesce(a.confirmada, 0) confirmada, coalesce(a.alguma_confirmada, 0) alguma_confirmada,
                    coalesce(h.retirada, 0) retirada, h.atualizado_em
                from pares_pagina p
                left join estado_atual a on a.unidade_id = p.unidade_id and a.vacina_id = p.vacina_id
                left join historico h on h.unidade_id = p.unidade_id and h.vacina_id = p.vacina_id
                order by
                """ + ordemFinal;
        NativeQuery<?> consulta = em.createNativeQuery(sql).unwrap(NativeQuery.class);
        consulta.addScalar("unidade_id", UUID.class).addScalar("unidade_nome", String.class)
                .addScalar("vacina_id", UUID.class).addScalar("vacina_nome", String.class)
                .addScalar("confirmada", Integer.class).addScalar("alguma_confirmada", Integer.class)
                .addScalar("retirada", Integer.class).addScalar("atualizado_em", OffsetDateTime.class);
        filtrar(consulta, unidadeId, vacinaId);
        consulta.setParameter("deslocamento", pagina.getOffset());
        consulta.setParameter("limite", pagina.getPageSize());
        var conteudo = consulta.getResultList().stream().map(linha -> {
            Object[] c = (Object[]) linha;
            return new Registro((UUID)c[0], (String)c[1], (UUID)c[2], (String)c[3],
                    ((Integer)c[4]) == 1, ((Integer)c[5]) == 1, ((Integer)c[6]) == 1, (OffsetDateTime)c[7]);
        }).toList();
        var contagem = em.createNativeQuery("select count(*) from unidade_saude u cross join vacina v where 1 = 1" + filtros);
        filtrar(contagem, unidadeId, vacinaId);
        long total = ((Number)contagem.getSingleResult()).longValue();
        return new PageImpl<>(conteudo, pagina, total);
    }

    private static void filtrar(jakarta.persistence.Query consulta, UUID unidadeId, UUID vacinaId) {
        if (unidadeId != null) consulta.setParameter("unidadeId", unidadeId);
        if (vacinaId != null) consulta.setParameter("vacinaId", vacinaId);
    }

    private static String ordenar(Pageable pagina, String prefixo) {
        Sort sort = pagina.getSort().isSorted() ? pagina.getSort()
                : Sort.by("unidadeNome", "vacinaNome");
        String campos = sort.stream().map(ordem -> {
            String coluna = ORDENACAO.get(ordem.getProperty());
            if (coluna == null) throw new IllegalArgumentException("Campo de ordenação não permitido");
            return prefixo + coluna + (ordem.isAscending() ? " asc" : " desc");
        }).collect(Collectors.joining(", "));
        return campos + ", " + prefixo + "unidade_id asc, " + prefixo + "vacina_id asc";
    }
}

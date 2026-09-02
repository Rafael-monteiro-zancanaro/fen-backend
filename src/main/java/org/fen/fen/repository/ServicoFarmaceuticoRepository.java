package org.fen.fen.repository;

import org.fen.fen.domain.ServicoFarmaceutico;
import org.fen.fen.domain.StatusServicoFarmaceutico;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ServicoFarmaceuticoRepository extends JpaRepository<ServicoFarmaceutico, UUID> {
    long countByStatusPersistido(StatusServicoFarmaceutico statusPersistido);

    @Query("""
            select count(s) from ServicoFarmaceutico s join s.acompanhamento a
            where a.atendimentoAtual = s
              and a.encerradoEm is null
              and s.statusPersistido = org.fen.fen.domain.StatusServicoFarmaceutico.AGUARDANDO_RETORNO
              and a.proximoRetorno >= :today
            """)
    long contarAguardandoRetornoAtivoEmOrAfter(@Param("today") java.time.LocalDate today);

    @Query("""
            select count(s) from ServicoFarmaceutico s join s.acompanhamento a
            where a.atendimentoAtual = s
              and a.encerradoEm is null
              and s.statusPersistido = org.fen.fen.domain.StatusServicoFarmaceutico.AGUARDANDO_RETORNO
              and a.proximoRetorno = :today
            """)
    long contarReconsultasAtivasEm(@Param("today") java.time.LocalDate today);

    @Query("""
            select count(s) from ServicoFarmaceutico s join s.acompanhamento a
            where a.atendimentoAtual = s
              and a.encerradoEm is null
              and s.statusPersistido = org.fen.fen.domain.StatusServicoFarmaceutico.AGUARDANDO_RETORNO
              and a.proximoRetorno < :today
            """)
    long contarExpiradosAtivosAntesDe(@Param("today") java.time.LocalDate today);

    @Query("""
            select count(s) from ServicoFarmaceutico s
            where s.cuidadosFarmaceuticos.glicemiaCapilar is not null
               or s.cuidadosFarmaceuticos.pressaoSistolica is not null
               or s.cuidadosFarmaceuticos.pressaoDiastolica is not null
               or s.cuidadosFarmaceuticos.temperaturaCorporal is not null
            """)
    long contarCuidadosFarmaceuticos();

    @Query("""
            select count(distinct ma.servicoFarmaceutico.id) from MedicamentoAtendimento ma
            where ma.tipoServico = org.fen.fen.domain.TipoServicoMedicamento.APLICACAO_INJETAVEIS
            """)
    long contarAplicacoesInjetaveis();

    @Query("""
            select count(distinct ma.servicoFarmaceutico.id) from MedicamentoAtendimento ma
            where ma.tipoServico = org.fen.fen.domain.TipoServicoMedicamento.INALOTERAPIA
            """)
    long contarInaloterapias();

    @Query("""
            select count(s) from ServicoFarmaceutico s
            where s.dadosServicosFarmaceuticos.assistenciaDomiciliar is not null
               or s.dadosServicosFarmaceuticos.acompanhamentoFarmacoterapeutico is not null
               or s.dadosServicosFarmaceuticos.indicacaoTranstornosMenores is not null
               or s.dadosServicosFarmaceuticos.sinaisESintomas is not null
               or exists (
                   select 1 from MedicamentoAtendimento ma
                   where ma.servicoFarmaceutico = s
                     and ma.tipoServico = org.fen.fen.domain.TipoServicoMedicamento.SERVICOS_FARMACEUTICOS
               )
            """)
    long contarServicosFarmaceuticos();
    @EntityGraph(attributePaths = {
            "paciente",
            "medicamentosAtendimento",
            "medicamentosAtendimento.medicamento",
            "acompanhamento"
    })
    @Query("select s from ServicoFarmaceutico s where s.id = :id")
    Optional<ServicoFarmaceutico> buscarDetalhe(@Param("id") UUID id);

    @EntityGraph(attributePaths = {"paciente", "acompanhamento"})
    List<ServicoFarmaceutico> findByAcompanhamentoIdOrderByNumeroRetornoAsc(UUID acompanhamentoId);

    @Query(value = """
            select s from ServicoFarmaceutico s join s.paciente p left join s.acompanhamento a
            where (:query = '' or lower(p.nome) like lower(concat('%', :query, '%'))
                   or p.cpf like concat('%', :digits, '%') or cast(s.codigo as string) like concat('%', :digits, '%'))
              and (:status = ''
                   or (:status = 'CONCLUIDO' and s.statusPersistido = org.fen.fen.domain.StatusServicoFarmaceutico.CONCLUIDO)
                   or (:status = 'AGUARDANDO_RETORNO'
                       and s.statusPersistido = org.fen.fen.domain.StatusServicoFarmaceutico.AGUARDANDO_RETORNO
                       and a.atendimentoAtual = s and a.encerradoEm is null and a.proximoRetorno >= :today)
                   or (:status = 'EXPIRADO'
                       and s.statusPersistido = org.fen.fen.domain.StatusServicoFarmaceutico.AGUARDANDO_RETORNO
                       and a.atendimentoAtual = s and a.encerradoEm is null and a.proximoRetorno < :today))
              and (:retornoHoje = false
                   or (s.statusPersistido = org.fen.fen.domain.StatusServicoFarmaceutico.AGUARDANDO_RETORNO
                       and a.atendimentoAtual = s and a.encerradoEm is null and a.proximoRetorno = :today))
            order by s.dataAtendimento desc, s.codigo desc
            """, countQuery = """
            select count(s) from ServicoFarmaceutico s join s.paciente p left join s.acompanhamento a
            where (:query = '' or lower(p.nome) like lower(concat('%', :query, '%'))
                   or p.cpf like concat('%', :digits, '%') or cast(s.codigo as string) like concat('%', :digits, '%'))
              and (:status = ''
                   or (:status = 'CONCLUIDO' and s.statusPersistido = org.fen.fen.domain.StatusServicoFarmaceutico.CONCLUIDO)
                   or (:status = 'AGUARDANDO_RETORNO'
                       and s.statusPersistido = org.fen.fen.domain.StatusServicoFarmaceutico.AGUARDANDO_RETORNO
                       and a.atendimentoAtual = s and a.encerradoEm is null and a.proximoRetorno >= :today)
                   or (:status = 'EXPIRADO'
                       and s.statusPersistido = org.fen.fen.domain.StatusServicoFarmaceutico.AGUARDANDO_RETORNO
                       and a.atendimentoAtual = s and a.encerradoEm is null and a.proximoRetorno < :today))
              and (:retornoHoje = false
                   or (s.statusPersistido = org.fen.fen.domain.StatusServicoFarmaceutico.AGUARDANDO_RETORNO
                       and a.atendimentoAtual = s and a.encerradoEm is null and a.proximoRetorno = :today))
            """)
    Page<ServicoFarmaceutico> listar(
            @Param("query") String query,
            @Param("digits") String digits,
            @Param("status") String status,
            @Param("retornoHoje") boolean retornoHoje,
            @Param("today") java.time.LocalDate today,
            Pageable pageable
    );

    @Query(value = """
            select s from ServicoFarmaceutico s join s.paciente p
            where (:cpf = '' or p.cpf = :cpf)
              and ((:medicamentoId is null and :lote = '') or exists (
                    select 1 from MedicamentoAtendimento ma
                    where ma.servicoFarmaceutico = s
                      and (:medicamentoId is null or ma.medicamento.id = :medicamentoId)
                      and (:lote = '' or lower(ma.lote) like lower(concat('%', :lote, '%')))
                  ))
              and (:dataAtendimento is null or s.dataAtendimento = :dataAtendimento)
            order by s.dataAtendimento desc, s.codigo desc
            """, countQuery = """
            select count(s) from ServicoFarmaceutico s join s.paciente p
            where (:cpf = '' or p.cpf = :cpf)
              and ((:medicamentoId is null and :lote = '') or exists (
                    select 1 from MedicamentoAtendimento ma
                    where ma.servicoFarmaceutico = s
                      and (:medicamentoId is null or ma.medicamento.id = :medicamentoId)
                      and (:lote = '' or lower(ma.lote) like lower(concat('%', :lote, '%')))
                  ))
              and (:dataAtendimento is null or s.dataAtendimento = :dataAtendimento)
            """)
    Page<ServicoFarmaceutico> buscarAvancado(
            @Param("cpf") String cpf,
            @Param("medicamentoId") UUID medicamentoId,
            @Param("lote") String lote,
            @Param("dataAtendimento") java.time.LocalDate dataAtendimento,
            Pageable pageable
    );
}

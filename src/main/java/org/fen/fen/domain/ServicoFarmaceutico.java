package org.fen.fen.domain;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.fen.fen.infra.AuditableEntity;
import org.hibernate.envers.Audited;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Entity
@Audited
@Getter
@Setter
@NoArgsConstructor
@Table(name = "servicofarmaceutico", uniqueConstraints = {
        @UniqueConstraint(name = "uk_servicofarmaceutico_codigo", columnNames = "codigo"),
        @UniqueConstraint(name = "uk_servicofarmaceutico_acompanhamento_retorno", columnNames = {"acompanhamentoid", "numeroretorno"})
})
public class ServicoFarmaceutico extends AuditableEntity {
    @Column(name = "codigo", nullable = false, unique = true) private Long codigo;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "pacienteid", nullable = false) private Paciente paciente;
    @Column(name = "dataatendimento", nullable = false) private LocalDate dataAtendimento;
    @Enumerated(EnumType.STRING) @Column(name = "statuspersistido", nullable = false, length = 30) private StatusServicoFarmaceutico statusPersistido;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "acompanhamentoid") private Acompanhamento acompanhamento;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "atendimentoanteriorid") private ServicoFarmaceutico atendimentoAnterior;
    @Column(name = "numeroretorno", nullable = false) private Integer numeroRetorno = 0;
    @Embedded private CuidadosFarmaceuticos cuidadosFarmaceuticos;
    @Embedded private DadosServicosFarmaceuticos dadosServicosFarmaceuticos;
    @OneToMany(mappedBy = "servicoFarmaceutico", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<MedicamentoAtendimento> medicamentosAtendimento = new ArrayList<>();

    public void adicionarMedicamento(MedicamentoAtendimento item) {
        item.setServicoFarmaceutico(this);
        medicamentosAtendimento.add(item);
    }

    public void removerMedicamento(MedicamentoAtendimento item) {
        medicamentosAtendimento.remove(item);
        item.setServicoFarmaceutico(null);
    }
}

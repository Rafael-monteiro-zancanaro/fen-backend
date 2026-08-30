package org.fen.fen.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.fen.fen.infra.AuditableEntity;
import org.hibernate.envers.Audited;

import java.time.LocalDate;

@Entity
@Audited
@Getter
@Setter
@NoArgsConstructor
@Table(name = "medicamentoatendimento")
public class MedicamentoAtendimento extends AuditableEntity {
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "servicofarmaceuticoid", nullable = false)
    private ServicoFarmaceutico servicoFarmaceutico;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "medicamentoid", nullable = false)
    private Medicamento medicamento;
    @Enumerated(EnumType.STRING)
    @Column(name = "tiposervico", nullable = false, length = 40)
    private TipoServicoMedicamento tipoServico;
    @Column(name = "lote", nullable = false, length = 100)
    private String lote;
    @Column(name = "validade", nullable = false)
    private LocalDate validade;
    @Column(name = "posologia", nullable = false, length = 500)
    private String posologia;
    @Column(name = "nomeprescritor", length = 150)
    private String nomePrescritor;
    @Column(name = "registroprescritor", length = 80)
    private String registroPrescritor;
}

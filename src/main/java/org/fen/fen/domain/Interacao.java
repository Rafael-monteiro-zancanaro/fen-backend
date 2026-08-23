package org.fen.fen.domain;

import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.fen.fen.infra.AuditableEntity;
import org.hibernate.envers.Audited;

@Getter
@Entity
@Audited
@NoArgsConstructor
@Table(
        name = "interacao",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_interacao_comorbidade_medicamento",
                columnNames = {"comorbidadeid", "medicamentoid"}
        )
)
public class Interacao extends AuditableEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "comorbidadeid", nullable = false)
    private Comorbidade comorbidade;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "medicamentoid", nullable = false)
    private Medicamento medicamento;

    public Interacao(Comorbidade comorbidade, Medicamento medicamento) {
        this.comorbidade = comorbidade;
        this.medicamento = medicamento;
    }
}

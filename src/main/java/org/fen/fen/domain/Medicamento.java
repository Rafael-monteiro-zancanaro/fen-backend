package org.fen.fen.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.fen.fen.infra.AuditableEntity;
import org.hibernate.envers.Audited;

@Getter
@Setter
@Entity
@Audited
@NoArgsConstructor
@Table(name = "medicamento")
public class Medicamento extends AuditableEntity {

    @Column(name = "nome", nullable = false, length = 254)
    private String nome;

    @Column(name = "unidademedida", nullable = false, length = 254)
    private String unidadeMedida;

    @Column(name = "viaadministracao", nullable = false, length = 254)
    private String viaAdministracao;

    public Medicamento(String nome, String unidadeMedida, String viaAdministracao) {
        this.nome = nome;
        this.unidadeMedida = unidadeMedida;
        this.viaAdministracao = viaAdministracao;
    }
}

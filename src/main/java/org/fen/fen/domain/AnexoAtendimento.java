package org.fen.fen.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.fen.fen.infra.AuditableEntity;
import org.hibernate.envers.Audited;

@Entity
@Audited
@Getter
@Setter
@NoArgsConstructor
@Table(name = "anexoatendimento")
public class AnexoAtendimento extends AuditableEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "servicofarmaceuticoid", nullable = false)
    private ServicoFarmaceutico servicoFarmaceutico;

    @Column(name = "nomeoriginal", nullable = false, length = 255)
    private String nomeOriginal;

    @Column(name = "chavearmazenamento", nullable = false, unique = true, length = 36)
    private String chaveArmazenamento;

    @Column(name = "contenttype", nullable = false, length = 255)
    private String contentType;

    @Column(name = "tamanho", nullable = false)
    private long tamanho;
}

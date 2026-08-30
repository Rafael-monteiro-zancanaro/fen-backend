package org.fen.fen.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
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
@Table(name = "acompanhamento")
public class Acompanhamento extends AuditableEntity {
    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "atendimentoinicialid", nullable = false, unique = true)
    private ServicoFarmaceutico atendimentoInicial;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "atendimentoatualid", nullable = false, unique = true)
    private ServicoFarmaceutico atendimentoAtual;

    @Column(name = "intervaloretornodias", nullable = false) private Integer intervaloRetornoDias;
    @Column(name = "quantidaderetornos", nullable = false) private Integer quantidadeRetornos;
    @Column(name = "proximoretorno") private LocalDate proximoRetorno;
    @Column(name = "encerradoem") private LocalDate encerradoEm;
    @Version @Column(name = "version", nullable = false) private Long version;
}

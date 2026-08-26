package org.fen.fen.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.fen.fen.infra.AuditableEntity;
import org.hibernate.envers.Audited;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDate;

@Entity
@Table(name = "funcionario")
@Audited
@EntityListeners(AuditingEntityListener.class)
@Getter
@Setter
@NoArgsConstructor
public class Funcionario extends AuditableEntity {

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "usuarioid", nullable = false, unique = true)
    private Usuario usuario;

    @Column(name = "nome", nullable = false, length = 150)
    private String nome;

    @Column(name = "cpf", nullable = false, unique = true, length = 11)
    private String cpf;

    @Column(name = "datanascimento")
    private LocalDate dataNascimento;

    @Column(name = "crf", length = 20)
    private String crf;

    @Column(name = "responsaveltecnico")
    private Boolean responsavelTecnico;

    @Enumerated(EnumType.STRING)
    @Column(name = "tipoestagio", length = 20)
    private TipoEstagio tipoEstagio;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "supervisorid")
    private Funcionario supervisor;

    @Column(name = "iniciovigencia")
    private LocalDate inicioVigencia;

    @Column(name = "fimvigencia")
    private LocalDate fimVigencia;

    @PrePersist
    @PreUpdate
    void normalizeCpf() {
        if (cpf != null) {
            cpf = cpf.replaceAll("\\D", "");
        }
    }
}

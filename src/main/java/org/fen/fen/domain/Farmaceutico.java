package org.fen.fen.domain;

import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.fen.fen.infra.AuditableEntity;
import org.fen.fen.validation.annotation.ValidCRF;
import org.hibernate.annotations.Audited;

import java.util.UUID;

@Data
@Entity
@Table(name = "farmaceutico")
@Audited
@EqualsAndHashCode(of = { "id" }, callSuper = false)
public class Farmaceutico extends AuditableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "crf", unique = true)
    @ValidCRF
    private String crf;

    @Column(name="responsaveltecnico")
    private Boolean responsavelTecnico;
}

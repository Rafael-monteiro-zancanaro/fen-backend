package org.fen.fen.domain;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import org.fen.fen.infra.AuditableEntity;
import org.fen.fen.validation.annotation.ValidCRF;
import org.hibernate.annotations.Audited;
import org.springframework.data.domain.Auditable;

import java.io.Serializable;
import java.util.UUID;

@Data
@Entity
@Table(name = "farmaceutico")
@Audited
public class Farmaceutico extends AuditableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private UUID id;

    @Column(name = "crf", unique = true)
    @ValidCRF
    private String crf;

    @Column(name="responsaveltecnico")
    private Boolean responsavelTecnico;
}

package org.fen.fen.domain;

import jakarta.persistence.*;
import lombok.Data;
import org.fen.fen.infra.AuditableEntity;

import java.util.List;

@Data
@Entity
@Table(name="medication")
public class Medication extends AuditableEntity {

    @Column(name="name")
    private String name;

    @Column(name="measurementunit")
    private String measurementUnit;

    @Column(name="administrationroute")
    private String administrationRoute;

    @ManyToMany(fetch=FetchType.LAZY, mappedBy = "medicationInteractions")
    private List<Comorbity> medicationInteractions;

}

package org.fen.fen.domain;

import jakarta.persistence.*;
import lombok.Data;
import org.fen.fen.infra.AuditableEntity;

import java.util.HashSet;
import java.util.Set;

@Data
@Entity
@Table(name="comorbity")
public class Comorbity extends AuditableEntity {

    @Column(name="name")
    private String name;

    @JoinTable(
            name="comorbitymedication",
            joinColumns = @JoinColumn(name="comorbity_id"),
            inverseJoinColumns = @JoinColumn(name="medication_id")
    )
    @ManyToMany(fetch=FetchType.LAZY, cascade = {CascadeType.PERSIST, CascadeType.MERGE})
    private Set<Medication> medicationInteractions = new HashSet<>();
}

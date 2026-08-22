package org.fen.fen.infra;

import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.UUID;

@Data
@MappedSuperclass
@EqualsAndHashCode(of = { "id" }, callSuper=false)
public abstract class WithSynteticId {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name="id", unique = true)
    public UUID id;
}

package org.fen.fen.domain;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.fen.fen.infra.AuditableEntity;
import org.hibernate.envers.Audited;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

@Getter
@Setter
@Entity
@Audited
@NoArgsConstructor
@Table(name = "comorbidade")
public class Comorbidade extends AuditableEntity {

    @Column(name = "nome", nullable = false, length = 254)
    private String nome;

    @Setter(lombok.AccessLevel.NONE)
    @OneToMany(mappedBy = "comorbidade", fetch = FetchType.LAZY,
            cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Interacao> interacoes = new ArrayList<>();

    public Comorbidade(String nome) {
        this.nome = nome;
    }

    public Interacao adicionarInteracao(Medicamento medicamento) {
        Interacao interacao = new Interacao(this, medicamento);
        interacoes.add(interacao);
        return interacao;
    }

    public void removerInteracao(UUID medicamentoId) {
        interacoes.removeIf(interacao ->
                interacao.getMedicamento().getId().equals(medicamentoId));
    }

    public List<Interacao> getInteracoes() {
        return Collections.unmodifiableList(interacoes);
    }
}

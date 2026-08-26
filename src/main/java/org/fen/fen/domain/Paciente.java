package org.fen.fen.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.fen.fen.infra.AuditableEntity;
import org.hibernate.envers.Audited;

import java.time.LocalDate;
import java.util.LinkedHashSet;
import java.util.Set;

@Entity
@Audited
@Getter
@Setter
@NoArgsConstructor
@Table(name = "paciente", uniqueConstraints = @UniqueConstraint(name = "uk_paciente_cpf", columnNames = "cpf"))
public class Paciente extends AuditableEntity {
    @Column(name = "nome", nullable = false, length = 150) private String nome;
    @Column(name = "cpf", nullable = false, length = 11) private String cpf;
    @Column(name = "datanascimento", nullable = false) private LocalDate dataNascimento;
    @Column(name = "celular", nullable = false, length = 11) private String celular;
    @Column(name = "genero", length = 20) private String genero;
    @Column(name = "cep", length = 8) private String cep;
    @Column(name = "endereco", length = 254) private String endereco;
    @Column(name = "bairro", length = 150) private String bairro;
    @Column(name = "cidade", length = 150) private String cidade;
    @Column(name = "estado", length = 2) private String estado;
    @Column(name = "telefone", length = 11) private String telefone;
    @Column(name = "nomeresponsavel", length = 150) private String nomeResponsavel;
    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(name = "pacientecomorbidade", joinColumns = @JoinColumn(name = "pacienteid"), inverseJoinColumns = @JoinColumn(name = "comorbidadeid"))
    private Set<Comorbidade> comorbidades = new LinkedHashSet<>();
}

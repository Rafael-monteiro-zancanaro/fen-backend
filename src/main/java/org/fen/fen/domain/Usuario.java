package org.fen.fen.domain;


import jakarta.persistence.*;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.fen.fen.infra.AuditableEntity;
import org.hibernate.annotations.Audited;
import org.hibernate.validator.constraints.Length;

import java.time.LocalDate;
import java.util.UUID;

@Data
@Entity
@Table(name="usuario")
@Audited
@EqualsAndHashCode(of = { "id" }, callSuper = false)
public class Usuario extends AuditableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name="nome")
    @NotBlank
    @Length(max = 255)
    private String nome;

    @Column(name="datanascimento")
    private LocalDate dataNascimento;

    @Column(name="email", unique = true)
    @Email(message = "O email informado é invalido!")
    @NotNull
    private String email;

    @Column(name="hashsenha")
    @NotNull
    private String hashSenha;

    @Column(name="cpf")
    @Length(max=11)
    @NotNull
    private String cpf;

}

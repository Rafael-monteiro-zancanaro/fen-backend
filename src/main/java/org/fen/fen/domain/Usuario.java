package org.fen.fen.domain;


import jakarta.persistence.*;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import org.fen.fen.infra.AuditableEntity;
import org.hibernate.annotations.Audited;
import org.hibernate.validator.constraints.Length;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;
import java.util.UUID;

@Data
@Entity
@Table(name="usuario")
@Audited
public class Usuario extends AuditableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private UUID id;

    @Column(name="nome")
    @NotBlank
    @Length(max = 255)
    private String nome;

    @Column(name="datanascimento")
    private LocalDate dataNascimento;

    @Column(name="email", unique = true)
    @Email(message = "O email informado é invalido!")
    private String email;

    @Column(name="hashsenha")
    private String hashSenha;

    @Column(name="cpf")
    @Length(max=11)
    private String cpf;

}

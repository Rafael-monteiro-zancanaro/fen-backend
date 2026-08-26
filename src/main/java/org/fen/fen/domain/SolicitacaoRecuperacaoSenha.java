package org.fen.fen.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.fen.fen.infra.AuditableEntity;
import org.hibernate.envers.Audited;
import org.hibernate.envers.NotAudited;

@Entity
@Audited
@Getter
@Setter
@NoArgsConstructor
@Table(name = "solicitacaorecuperacaosenha")
public class SolicitacaoRecuperacaoSenha extends AuditableEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "usuarioid", nullable = false)
    private Usuario usuario;

    @NotAudited
    @Column(name = "novasenhahash", nullable = false, length = 100)
    private String novaSenhaHash;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private StatusSolicitacaoRecuperacaoSenha status;

    @Version
    @Column(name = "version", nullable = false)
    private long version;

    public SolicitacaoRecuperacaoSenha(Usuario usuario, String novaSenhaHash) {
        this.usuario = usuario;
        this.novaSenhaHash = novaSenhaHash;
        this.status = StatusSolicitacaoRecuperacaoSenha.PENDENTE;
    }
}

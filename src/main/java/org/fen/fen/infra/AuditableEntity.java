package org.fen.fen.infra;

import jakarta.persistence.Column;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.MappedSuperclass;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedBy;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedBy;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;
import org.hibernate.envers.Audited;

import java.time.LocalDateTime;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

@Data
@MappedSuperclass
@Audited
@AllArgsConstructor
@NoArgsConstructor
@EntityListeners(AuditingEntityListener.class)
@EqualsAndHashCode(callSuper=false)
public abstract class AuditableEntity extends WithSynteticId {

    @CreatedDate
    @Column(nullable = false, updatable = false, name="createdat")
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(name="updatedat")
    private LocalDateTime updatedAt;

    @CreatedBy
    @Column(name="createdby")
    private String createdBy;

    @LastModifiedBy
    @Column(name="updatedby")
    private String updatedBy;

    @PrePersist
    void applyCreator() {
        String auditor = currentAuditor();
        if (createdBy == null) {
            createdBy = auditor;
        }
        updatedBy = auditor;
    }

    @PreUpdate
    void applyUpdater() {
        updatedBy = currentAuditor();
    }

    public static String currentAuditor() {
        var authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()
                || authentication instanceof AnonymousAuthenticationToken) {
            return "Sistema";
        }
        return authentication.getName();
    }
}

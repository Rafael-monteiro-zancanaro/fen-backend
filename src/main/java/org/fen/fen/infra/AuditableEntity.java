package org.fen.fen.infra;

import jakarta.persistence.Column;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.MappedSuperclass;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedBy;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedBy;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

@Data
@MappedSuperclass
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
}

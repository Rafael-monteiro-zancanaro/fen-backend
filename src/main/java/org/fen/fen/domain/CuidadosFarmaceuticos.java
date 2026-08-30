package org.fen.fen.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Embeddable
@Getter
@Setter
public class CuidadosFarmaceuticos {
    @Column(name = "glicemiacapilar", precision = 10, scale = 2) private BigDecimal glicemiaCapilar;
    @Column(name = "pressaosistolica", precision = 10, scale = 2) private BigDecimal pressaoSistolica;
    @Column(name = "pressaodiastolica", precision = 10, scale = 2) private BigDecimal pressaoDiastolica;
    @Column(name = "temperaturacorporal", precision = 5, scale = 2) private BigDecimal temperaturaCorporal;
}

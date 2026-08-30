package org.fen.fen.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.Getter;
import lombok.Setter;

@Embeddable
@Getter
@Setter
public class DadosServicosFarmaceuticos {
    @Column(name = "assistenciadomiciliar")
    private Boolean assistenciaDomiciliar;
    @Column(name = "acompanhamentofarmacoterapeutico")
    private Boolean acompanhamentoFarmacoterapeutico;
    @Column(name = "indicacaotranstornosmenores")
    private Boolean indicacaoTranstornosMenores;
    @Column(name = "sinaisesintomas", length = 4000)
    private String sinaisESintomas;
}

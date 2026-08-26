package org.fen.fen.repository;

import org.fen.fen.domain.Comorbidade;
import org.fen.fen.domain.Interacao;
import org.fen.fen.domain.Medicamento;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.jdbc.Sql;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@Sql("/datasets/cadastros-repository.sql")
class MedicamentoInteracaoRepositoryTest extends BaseRepositoryTest {

    @Autowired
    private MedicamentoRepository medicamentoRepository;

    @Autowired
    private ComorbidadeRepository comorbidadeRepository;

    @Autowired
    private InteracaoRepository interacaoRepository;

    @Test
    void pesquisaMedicamentoPorNomeParaAutocomplete() {
        medicamentoRepository.saveAndFlush(new Medicamento("Dipirona", "mg", "Oral"));
        medicamentoRepository.saveAndFlush(new Medicamento("Ibuprofeno", "mg", "Oral"));

        assertThat(medicamentoRepository.autocomplete("DIPI", PageRequest.of(0, 8)))
                .extracting(Medicamento::getNome)
                .containsExactly("Dipirona");
    }

    @Test
    void carregaComorbidadeComInteracoesEMedicamentos() {
        Medicamento medicamento = medicamentoRepository.saveAndFlush(
                new Medicamento("Dipirona", "mg", "Oral"));
        Comorbidade comorbidade = new Comorbidade("Hipertensão");
        comorbidade.adicionarInteracao(medicamento);
        comorbidade = comorbidadeRepository.saveAndFlush(comorbidade);

        Comorbidade detalhe = comorbidadeRepository.buscarDetalhe(comorbidade.getId()).orElseThrow();

        assertThat(detalhe.getInteracoes())
                .extracting(interacao -> interacao.getMedicamento().getNome())
                .containsExactly("Dipirona");
    }

    @Test
    void bancoImpedeDuplicidadeDoParComorbidadeMedicamento() {
        Medicamento medicamento = medicamentoRepository.saveAndFlush(
                new Medicamento("Dipirona", "mg", "Oral"));
        Comorbidade comorbidade = comorbidadeRepository.saveAndFlush(new Comorbidade("Hipertensão"));
        interacaoRepository.saveAndFlush(new Interacao(comorbidade, medicamento));

        assertThatThrownBy(() ->
                interacaoRepository.saveAndFlush(new Interacao(comorbidade, medicamento)))
                .isInstanceOf(DataIntegrityViolationException.class);
    }
}

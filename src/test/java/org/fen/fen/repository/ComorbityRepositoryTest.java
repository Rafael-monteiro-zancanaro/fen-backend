package org.fen.fen.repository;

import org.assertj.core.api.Assertions;
import org.fen.fen.domain.Comorbity;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.jdbc.Sql;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Sql(scripts = "/datasets/comorbity-repository.sql")
public class ComorbityRepositoryTest extends BaseRepositoryTest {

    @Autowired
    private ComorbityRepository repository;

    private static final UUID HIPERTENSAO_ID = UUID.fromString("0b0f9147-3675-401b-93c1-4d26f24b7d66");


    @Test
    void findAll() {
        List<Comorbity> comorbities = this.repository.findAll();
        Assertions.assertThat(comorbities).isNotEmpty()
                .hasSize(3);
    }

    @Test
    void findById() {
        Optional<Comorbity> hipertensao = this.repository.findById(HIPERTENSAO_ID);
        Assertions.assertThat(hipertensao).isNotEmpty();
        Assertions.assertThat(hipertensao.get())
                .extracting(Comorbity::getName)
                .isEqualTo("Hipertensão");
    }

    @Test
    void save() {
        Comorbity comorbity = new Comorbity();
        comorbity.setName("Insuficiência Renal");

        Comorbity saved = this.repository.saveAndFlush(comorbity);

        Assertions.assertThat(saved.getId()).isNotNull();
        Assertions.assertThat(saved.getName())
                .isNotNull()
                .isEqualTo(comorbity.getName());
    }

    @Test
    void update() {
        Optional<Comorbity> hipertensao = this.repository.findById(HIPERTENSAO_ID);
        Assertions.assertThat(hipertensao).isNotEmpty();
        
        Comorbity updating = hipertensao.get();
        final String oldName = updating.getName();
        updating.setName("Hipertensão Atualizada");

        Comorbity updated = this.repository.saveAndFlush(updating);

        Assertions.assertThat(updated.getId()).isNotNull()
                        .isEqualTo(HIPERTENSAO_ID);

        Assertions.assertThat(updated.getName())
                .isNotNull()
                .isNotEqualTo(oldName);
    }

    @Test
    void deleteById() {
        Optional<Comorbity> hipertensao = this.repository.findById(HIPERTENSAO_ID);
        Assertions.assertThat(hipertensao).isNotEmpty();

        this.repository.deleteById(hipertensao.get().getId());

        Optional<Comorbity> shouldBeEmpty = this.repository.findById(HIPERTENSAO_ID);
        Assertions.assertThat(shouldBeEmpty).isEmpty();

    }
}

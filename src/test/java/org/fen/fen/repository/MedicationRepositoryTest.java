package org.fen.fen.repository;

import org.assertj.core.api.Assertions;
import org.fen.fen.domain.Medication;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.jdbc.Sql;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Sql(scripts = "/datasets/medication-repository.sql")
public class MedicationRepositoryTest extends BaseRepositoryTest {

    @Autowired
    private MedicationRepository repository;

    private static final UUID FUROSEMIDA_ID = UUID.fromString("550e8400-e29b-41d4-a716-446655440005");


    @Test
    void findAll() {
        List<Medication> medications = this.repository.findAll();
        Assertions.assertThat(medications).isNotEmpty()
                .hasSize(10);
    }

    @Test
    void findById() {
        Optional<Medication> furosemida = this.repository.findById(FUROSEMIDA_ID);
        Assertions.assertThat(furosemida).isNotEmpty();
        Assertions.assertThat(furosemida.get())
                .extracting(Medication::getName)
                .isEqualTo("Furosemida");
    }

    @Test
    void save() {
        Medication medication = new Medication();
        medication.setName("Paracetamol");
        medication.setAdministrationRoute("Oral");
        medication.setMeasurementUnit("mg");

        Medication saved = this.repository.saveAndFlush(medication);

        Assertions.assertThat(saved.getId()).isNotNull();
        Assertions.assertThat(saved.getName())
                .isNotNull()
                .isEqualTo(medication.getName());
    }

    @Test
    void update() {
        Optional<Medication> furosemida = this.repository.findById(FUROSEMIDA_ID);
        Assertions.assertThat(furosemida).isNotEmpty();

        Medication updating = furosemida.get();
        final String oldName = updating.getName();
        updating.setName("Furosemida Oral");

        Medication updated = this.repository.saveAndFlush(updating);

        Assertions.assertThat(updated.getId()).isNotNull()
                        .isEqualTo(FUROSEMIDA_ID);

        Assertions.assertThat(updated.getName())
                .isNotNull()
                .isNotEqualTo(oldName);
    }

    @Test
    void deleteById() {
        Optional<Medication> hipertensao = this.repository.findById(FUROSEMIDA_ID);
        Assertions.assertThat(hipertensao).isNotEmpty();

        this.repository.deleteById(hipertensao.get().getId());

        Optional<Medication> shouldBeEmpty = this.repository.findById(FUROSEMIDA_ID);
        Assertions.assertThat(shouldBeEmpty).isEmpty();

    }
}

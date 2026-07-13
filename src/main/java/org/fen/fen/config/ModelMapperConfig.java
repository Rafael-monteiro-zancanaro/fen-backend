package org.fen.fen.config;

import org.fen.fen.infra.AuditorAwareImpl;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.domain.AuditorAware;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

@Configuration
@EnableJpaAuditing(auditorAwareRef = "getAuditorAwareImpl")
public class ModelMapperConfig {

    @Bean
    public AuditorAware<String> getAuditorAwareImpl() {
        return new AuditorAwareImpl();
    }
}
